# 2026-08-14 — Record the real visitor IP behind Cloudflare

- Date: 2026-08-14
- GitHub Issue: #169
- Status: Implemented

## Goal

Store the actual visitor IP instead of the Cloudflare edge IP, on every path that
consumes a client address: the request log line, the statistics write path, and
the curator's conversation id.

## Non-goals

- Parsing the `X-Forwarded-For` chain or registering Cloudflare ranges in
  `RemoteIpValve`'s `trustedProxies`. One header is enough, and the range list
  needs maintenance every time Cloudflare changes it.
- Replacing the GeoLite2 lookup with `CF-IPCountry`.
- Backfilling rows already written with an edge IP. The original address is gone.
- Restricting origin access (firewall / Cloudflare Tunnel). Infrastructure work,
  tracked in the issue as a deployment prerequisite.
- Correcting `api-limit-mvc`'s IP factor. The library reads the address itself
  from `application-apilimit.yml`'s `factor: IP`, not from anything this
  repository passes it, so the chat rate limit stays edge-wide until the library
  is fixed.

## Context / Constraints

- `RequestLoggingFilter:28,48` and `ChatRestController:26` are the only
  `getRemoteAddr()` call sites.
- The chat address is not just logged: `ChatService` forwards it to
  `BlogAgent.prompt(message, conversationId)`, where it becomes
  `ChatMemory.CONVERSATION_ID` (`BlogAgent:119-125`). Behind the edge every
  visitor shares one conversation, so context leaks between them. This is the
  most damaging symptom of the three.
- `application.yml:3` sets `forward-headers-strategy: native`. Tomcat's
  `RemoteIpValve` only walks past proxies matching `internalProxies` (private
  ranges); Cloudflare edge IPs are public, so the walk stops at the edge.
- `V1__initial_schema.sql:66` declares `request_statistics.ip` as `VARCHAR(15)` —
  IPv4-only. Cloudflare passes the visitor's real IPv6 address for IPv6 clients,
  so this column must widen to 45 before the switch, otherwise
  `persistStatistics()` loses a whole 5-minute batch on the first IPv6 visitor
  (`saveAll` of the queue snapshot runs in one transaction).
- `chat_conversations.ip` is already `VARCHAR(45)`; no change needed.
- `ddl-auto: none` in production with Flyway; tests run H2 with `create-drop`, so
  the entity `@Column` length and the migration must agree.
- `CF-Connecting-IP` is attacker-controllable if the origin is reachable
  directly. Cloudflare overwrites any client-supplied value, so the header is
  trustworthy exactly when the origin only accepts Cloudflare traffic.

## Approach (Checklist)

- [x] **Step 0: Recon** — filter, chat controller, `RequestStatisticsService`,
      `GeoIpLocationResolver`, `application*.yml`, Flyway migrations, entity
      column definitions, existing test conventions.
- [x] **Step 1: Resolver** — `global/util/ClientIpResolver`: static
      `resolve(HttpServletRequest)`. Returns the `CF-Connecting-IP` value when it
      is present and parses as an IP literal, otherwise `getRemoteAddr()`. The
      literal check keeps a forged host name out of the `ip` column and away from
      `GeoIpLocationResolver`.
- [x] **Step 2: Call sites** — `RequestLoggingFilter` (log line and
      `recordRequest`) and `ChatRestController` route through the resolver.
- [x] **Step 3: Schema** — `V4__widen_request_statistics_ip.sql` widens
      `ip` to `VARCHAR(45)`; `RequestStatistics.ip` gains `@Column(length = 45)`
      so the H2 test schema matches.
- [x] **Step 4: Tests** — `ClientIpResolverTest` for header present / absent /
      blank / non-IP; `RequestLoggingFilterTest` proving the header reaches
      `recordRequest`.

## Validation

- **Commands to run:** `./gradlew test`
- **Result:** `BUILD SUCCESSFUL`, 426 tests, 0 failures. `OPENAI_API_KEY` must be
  present or 28 `@SpringBootTest` contexts fail to load on an unrelated missing
  bean — a pre-existing condition, see #134 and #150.

## Risks & Rollback

- **Risks:**
  - The header is trusted without checking the peer. Until the origin only
    accepts Cloudflare traffic, a direct request carrying a forged
    `CF-Connecting-IP` can skew the statistics and pick its own conversation id.
    Reaching another visitor's conversation still means guessing their address,
    and today every visitor already shares one, so this is not a regression — but
    it is a real precondition and the issue calls it out.
  - The stored `ip` distribution changes shape at deploy time: rows before and
    after the change are not comparable, and IPv6 values start appearing.
- **Rollback steps:** `git revert` the commit. The migration is additive in
  effect (a widening `ALTER COLUMN TYPE`), so reverting the code leaves the wider
  column in place and nothing breaks.

## Open Questions

- Is the origin already behind Cloudflare Tunnel, or does it accept direct
  inbound traffic? The fix is correct either way, but the trust assumption only
  holds in the first case.
