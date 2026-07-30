# 2026-07-30 — Country-level geo analytics for request statistics

- Date: 2026-07-30
- GitHub Issue: #118
- Status: Draft
- Base branch: `enhancement/112` (needs `1c6ba37`)

## Goal

- Resolve the stored client `ip` to an ISO country code with a local MaxMind
  GeoLite2 database, at insert time, with no external call on the request path.
- Add `request_statistics.country_code CHAR(2)`, so reads are a plain
  `GROUP BY country_code`.
- Start and serve normally when the mmdb file is absent; `country_code` stays
  null (test runtime and developer machines have no mmdb).
- Provide a chunked backfill for the ~178k existing rows / ~4.2k distinct IPs.
- Console dashboard stage one: paginated requests-per-country table with a flag,
  honouring the `days` filter and excluding bots.
- Decide and document raw-`ip` retention and mmdb distribution/refresh.

## Non-goals

- City-level coordinates, map pins, world-map choropleth. The country table
  carries the same information at a fraction of the rendering cost.
- IP-based access control or geo blocking.
- Implementing the raw-`ip` retention job (needs its own scheduled job — see
  "Follow-up issue" below).

## Context / Constraints

- Prerequisite verification passed (issue comment, 2026-07-30): 177,962 rows,
  `COUNT(DISTINCT ip)` = 4,227, 7 private-range IPs, oldest row 2025-12-17.
  `X-Forwarded-For` forwarding works and the stored `ip` is the real client IP.
- `ddl-auto: none`; `src/main/resources/sql/schema.sql` is an append-only,
  manually applied PostgreSQL migration log. Tests run on H2 and never execute
  it.
- #112 already delivered `BotDetector`, `is_bot`, `duration_ms`, and the single
  7-arg `recordRequest`. Bots are excluded with `AND r.isBot = false`.
- Issues #113–#117 run in parallel and are read-only. Therefore: no new methods
  on `RequestStatisticsRepository`, no edits to `ConsoleApiController`, and only
  minimum tab wiring in `dashboard.html`; everything else lands in new files.
- The MaxMind licence key (`MAXMIND_LICENCE_KEY`, British spelling) is a secret.
  It is only needed to *download* the database, never at runtime.
- Only one new backend dependency is allowed: `com.maxmind.geoip2:geoip2`.
  No new frontend dependencies (flag rendering uses Unicode regional indicators).

## Approach (Checklist)

- [x] **Step 0: Recon** — entity, service, filter, repository,
      `ConsoleApiController`, `ConsoleController`, `dashboard.html`,
      `schema.sql`, `application*.yml`, `WebSecurityConfig`, `Dockerfile`,
      `ci.yml`, existing service test.
- [x] **Step 1: Resolver**
  - `manage/service/GeoIpCountryResolver.java`: `@Component`, opens the mmdb
    from `app.geoip.database-path` once with `CHMCache`. Missing/unreadable file
    or blank path → warn once, `resolveCountryCode` returns null forever.
  - Reject anything that is not an IP literal before `InetAddress.getByName`,
    so a spoofed `X-Forwarded-For` hostname can never cause a DNS lookup on the
    request path.
- [x] **Step 2: Write path**
  - `RequestStatistics`: add `countryCode` (`country_code`, `CHAR(2)`).
  - `RequestStatisticsService.recordRequest`: resolve via the injected resolver.
    Signature unchanged, so sibling read work is unaffected.
  - `sql/schema.sql`: append `ALTER TABLE ... ADD COLUMN country_code CHAR(2)`
    plus an index.
- [x] **Step 3: Read path (own files)**
  - `manage/domain/CountryStat.java` record.
  - `manage/repository/RequestGeoStatisticsRepository.java`: paginated
    `GROUP BY country_code`, distinct-IP lookup and per-IP update for backfill.
  - `manage/service/RequestGeoStatisticsService.java`.
  - `manage/controller/ConsoleGeoApiController.java`:
    `GET /api/admin/console/geo/countries`, `POST /api/admin/console/geo/backfill`.
- [x] **Step 4: Dashboard**
  - `templates/console/fragments/geo.html`: card, table, pagination, bot toggle,
    backfill button. Exposes `window.loadGeo(days)`.
  - `dashboard.html`: one nav tab button, one pane div, two dispatch lines.
- [x] **Step 5: Distribution / config**
  - `Dockerfile`: download GeoLite2-Country in the build stage with
    `--secret id=MAXMIND_LICENCE_KEY`, copy the mmdb into the runtime image.
  - `ci.yml`: pass the secret. `.env.example`, `.gitignore`, `application.yml`.
- [x] **Step 6: Tests** — `GeoIpCountryResolverTest` (fallback, literal guard),
      extended `RequestStatisticsServiceTest`, `RequestGeoStatisticsServiceTest`.

## Decisions

### mmdb distribution and refresh

Chosen: **download during image build, using the licence key as a build secret**
(the pattern the Dockerfile already uses for GitHub Packages credentials).

- Build stage runs `curl` against
  `https://download.maxmind.com/app/geoip_download?edition_id=GeoLite2-Country`
  with `--mount=type=secret,id=MAXMIND_LICENCE_KEY`, unpacks the single
  `.mmdb`, and the runtime stage `COPY --from=build`s it to
  `/app/geoip/GeoLite2-Country.mmdb`.
- The download is best-effort: no secret, no network, or a MaxMind error leaves
  the file absent and the build still succeeds. The application then runs with
  `country_code` null. This keeps the image build reproducible for anyone
  without the licence key.
- Rejected: mounted volume. It needs a host-side `geoipupdate` cron plus a
  compose/k8s volume definition — a second moving part outside the repository —
  for a database whose staleness costs at most a few misattributed rows.
  `GEOIP_DATABASE_PATH` still exists as an env override, so a mounted volume
  remains available without a code change if that trade-off ever flips.
- **Refresh:** MaxMind publishes GeoLite2-Country twice weekly (Tue/Fri). The
  image is rebuilt on every push to `main`, so the database is refreshed on each
  deploy. If deploys ever go quiet for a long stretch, add a weekly
  `schedule:` trigger to `.github/workflows/ci.yml`; that is a deployment
  behaviour change and is deliberately not part of this issue. Country-level
  data drifts slowly — a several-week-old database is accurate enough for
  reader-geography statistics.

### Raw IP retention policy

Adopted policy, documented here:

- Raw `ip` is retained for **90 days** after `created_at`, then replaced in
  place by a keyed SHA-256 hash truncated to 16 hex characters.
- 90 days is the widest window the console offers (`1/7/30/90일`), and the
  "Top IPs" panel is the only feature that needs the raw value. Beyond that
  window the raw address serves no product purpose.
- Hashing rather than deleting: distinct-visitor counts and repeat-abuse
  grouping survive, while the value stops being a network identifier. The key
  lives with the other application secrets, never in the repository.
- `country_code` is resolved at insert time, so geo statistics are unaffected by
  hashing or deletion — that is the point of this issue.

**Not implemented here.** It needs a scheduled job plus a one-off migration over
177,962 existing rows, which is neither small nor reversible, so per the issue's
own instruction it goes to a follow-up issue (drafted below).

## Follow-up issue (to file, not yet created)

> **Title:** Enforce 90-day raw IP retention on request_statistics
>
> **Problem.** `request_statistics.ip` and `chat_conversations.ip` are personal
> data retained indefinitely in plaintext. Since #118, `country_code` is
> resolved at insert time, so the raw address is no longer needed for geography.
>
> **Proposal.** Adopt the policy documented in
> `.plan/issues/2026-07-30-issue-118-country-geo-analytics.md`: retain raw `ip`
> for 90 days, then overwrite it in place with `left(hex(hmac_sha256(ip, key)), 16)`.
>
> **Scope.**
> - A `@Scheduled` daily job that hashes rows older than 90 days in chunks
>   (bounded `UPDATE ... WHERE created_at < now() - interval '90 days' AND
>   length(ip) <> 16`), so it is restartable and never loads the table.
> - A one-off backfill of the existing rows older than 90 days.
> - Widen `ip VARCHAR(15)` — it is already too narrow for IPv6 and for a
>   16-char hash prefix; `chat_conversations.ip` is `VARCHAR(45)`.
> - Decide whether `chat_conversations.ip` follows the same policy.
> - Document the retention window in the site privacy notice.
>
> **Risks.** The hash is irreversible: once a row is hashed, per-IP forensics
> for that period are gone. Run the one-off backfill only after the scheduled
> job has been verified on a copy.

## Validation

- **Commands to run:** `./gradlew test`
- **Expected output:** `BUILD SUCCESSFUL`, no pre-existing failures introduced.
- The graceful-fallback path is covered by a test, because no test environment
  has the mmdb file.
- Not verifiable here: an actual mmdb lookup returning a real country, the
  Docker build (no licence key or Docker daemon in this environment), and the
  backfill against production.

## Risks & Rollback

- **Risks:**
  - The resolver is constructed at startup; a corrupt mmdb must not break boot.
    Handled by catching everything during construction and degrading to null.
  - Per-request lookup cost. Mitigated by `CHMCache` in the MaxMind reader and
    by the existing 5-minute in-memory flush; the lookup is an in-process
    binary-tree walk over a memory-mapped file.
  - Country attribution is approximate: VPNs, mobile carrier gateways, and
    proxies all misattribute. Directional statistics only.
  - The backfill issues one `UPDATE` per distinct IP. Chunked by distinct IP,
    default 500 IPs per call, so it never loads the row set into memory.
- **Rollback steps:** `git revert` the commits. `country_code` is additive and
  nullable, so leaving the column in place breaks nothing.

## Open Questions

- Should unresolvable IPs (private ranges, ranges absent from GeoLite2) be
  marked with a sentinel such as `ZZ` so the backfill stops revisiting them?
  Currently 7 rows, so not worth the column semantics.
- Bot exclusion in the country table defaults to on and is a checkbox. If the
  bot share turns out to be dominant, the dashboard summary cards should
  probably show it too — that belongs to the read-only sibling issues.
