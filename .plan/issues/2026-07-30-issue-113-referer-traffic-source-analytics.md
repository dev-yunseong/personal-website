# 2026-07-30 — Add referer-based traffic source analytics

- Date: 2026-07-30
- GitHub Issue: #113
- Status: Draft

## Goal

- Aggregate the already-stored `referer` column by host and expose it in the
  admin console.
- Classify each host into a channel: search engines, social & community,
  internal (own host), other referral, direct (no referer).
- New console tab: channel share chart plus a paginated list of top external
  domains, honouring the existing `days` filter and a bot-exclusion toggle.
- Keep the null/direct bucket visible as its own channel; in production the
  referer is absent for most rows.

## Non-goals

- Session/visitor-level attribution, UTM parsing (query strings are not stored).
- Schema, entity, filter, or `recordRequest` changes — read-only feature.
- Touching `RequestStatisticsRepository`, `RequestStatisticsService`, or
  `ConsoleApiController`; issues #114–#118 run in parallel on those files.

## Context / Constraints

- Base branch is `enhancement/112` (contains `1c6ba37`), which supplies
  `BotDetector`, the `is_bot` / `duration_ms` columns, and the widened
  collection surface.
- File ownership: own repository, service, domain records, `@RestController`, and
  console fragment. `dashboard.html` gets wiring only (one nav button, one pane
  include, two tab-dispatch lines).
- Aggregation must happen in SQL. One `GROUP BY r.referer, r.isBot` query
  returns one row per distinct referer per bot flag, so the app never sees raw
  rows and the bot toggle needs no second query.
- Host parsing cannot be done in portable SQL (JPQL has no regex, and stored
  values include scheme-less referers), so the grouped rows are folded to hosts
  and channels in Java. Unit-testable parsing is also what the issue asks for.
- `idx_request_statistics_created_at` already exists and serves the
  `created_at >= ?` predicate; no new index is added.
- Own host comes from the existing `app.base-url` property, parsed with the same
  host parser.

## Approach (Checklist)
- [x] **Step 0: Recon** — read `RequestStatisticsRepository`,
      `RequestStatisticsService`, `ConsoleApiController`, `UriStat`,
      `RequestStatistics`, `BotDetector`, `dashboard.html`, `schema.sql`,
      `application.yml`, existing manage tests. Baseline `./gradlew test` green
      only with `OPENAI_API_KEY` set; without it 10 pre-existing AI-context
      tests fail for a missing secret.
- [ ] **Step 1: Implementation**
  - `manage/domain/RefererSource.java`: `hostOf(String)` + `of(referer, internalHost)`
    returning a `(host, Channel)` pair; nested `Channel` enum with display labels.
  - `manage/domain/RefererGroupCount.java`: repository projection
    `(referer, bot, requestCount)`.
  - `manage/domain/RefererStat.java`: `(label, requestCount)` output row, used for
    both channels and domains.
  - `manage/domain/RefererBreakdown.java`: `(channels, domains page)`.
  - `manage/repository/RefererStatisticsRepository.java`: one JPQL group-by query.
  - `manage/service/RefererStatisticsService.java`: fold rows into channel totals
    and external-domain counts, sort, paginate.
  - `manage/controller/RefererApiController.java`: `GET /api/admin/console/referer`.
  - `templates/console/fragments/referer.html`: tab pane markup, doughnut chart on
    the already-loaded Chart.js, domain table, pagination, bot toggle, own JS.
  - `templates/console/dashboard.html`: nav button, pane include, two dispatch lines.
- [ ] **Step 2: Tests**
  - `RefererSourceTest`: full URLs, scheme-less values, ports, uppercase hosts,
    malformed input, empty string, null, every channel including internal
    subdomains and the ambiguous `x.com` / `t.co` hosts.
  - `RefererStatisticsServiceTest`: channel totals, bot exclusion, all five
    channels always present, external-domain ordering and pagination, page clamp.
  - `RefererStatisticsRepositoryTest` (`@DataJpaTest`, H2): the group-by query
    actually runs and groups NULL referers, since no other test loads a context.
- [ ] **Step 3: Rollout / Rollback** — read-only feature, no migration. Revert the
      commits to remove it.

## Validation
- **Commands to run:** `OPENAI_API_KEY=<dummy> ./gradlew test`
- **Expected output:** `BUILD SUCCESSFUL`; without the key the same 10
  pre-existing AI-context failures remain.

## Risks & Rollback
- **Risks:** the grouped result set is bounded by distinct referer count, not row
  count. Referer spam could inflate that cardinality; the upgrade path is
  host extraction in SQL (PostgreSQL `regexp_substr`) at the cost of portability
  and of the unit-tested parser.
- Channel classification matches host labels, so an unrelated host containing a
  brand label (`google.attacker.example`) is misclassified. Cost is one
  mislabelled row in an admin-only view.
- **Rollback steps:** `git revert` the commits; nothing persistent changes.

## Open Questions
- A referer with no parseable host falls into `DIRECT` rather than a sixth
  "malformed" bucket. Revisit if production shows meaningful volume there.
