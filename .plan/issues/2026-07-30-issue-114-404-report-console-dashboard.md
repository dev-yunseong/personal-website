# 2026-07-30 — Add 404 report to console dashboard

- Date: 2026-07-30
- GitHub Issue: #114
- Status: Done

## Goal

Give the admin console an actionable 404 report: per-URI occurrence count,
first/last seen, a representative referer, and a split between internal
(own-host) referers, external referers, and direct hits. Supports the existing
`days` period filter, bot exclusion, sorting (count / most recent), and
pagination.

## Non-goals

- Automatic redirect rules or auto-fixing broken links.
- 5xx alerting/monitoring (#92).
- Schema, entity, `RequestLoggingFilter`, or `recordRequest` changes.

## Context / Constraints

- Read-only aggregation over `request_statistics`; `status_code = 404` rows only.
- Aggregation must happen in SQL. Production table is ~178k rows and growing, so
  no full-table fetch into the app.
- Branch `enhancement/114` is based on `enhancement/112` (commit `1c6ba37`),
  which adds `is_bot` / `duration_ms` and widens collection to `/`,
  `/public/**`, `/api/public/**`.
- Five sibling issues are in flight, so file ownership is strict: new
  repository / service / domain / controller / fragment files only.
  `dashboard.html` gets nav tab + pane include + tab dispatch lines and nothing
  else.
- Collection is centred on public routes, so scanner-style 404s are mostly not
  recorded. The UI must say so.
- No new dependencies. Reuse the already-loaded Chart.js if charting (not
  needed here).

## Approach (Checklist)
- [x] **Step 0: Recon** — read `RequestStatisticsRepository`,
  `RequestStatisticsService`, `ConsoleApiController`, `UriStat`,
  `dashboard.html`, `RequestStatistics`, `schema.sql`, `ConsoleController`.
- [x] **Step 1: Implementation**
  - `manage/domain/NotFoundStat.java` — aggregation row record.
  - `manage/repository/NotFoundStatisticsRepository.java` — two JPQL
    constructor-expression queries (count desc, last-seen desc), differing only
    in `ORDER BY`, sharing the projection via a compile-time constant.
  - `manage/service/NotFoundReportService.java` — derives the period start,
    resolves the internal-referer LIKE pattern from `app.base-url`, picks the
    sort, applies bot exclusion, paginates.
  - `manage/controller/NotFoundReportApiController.java` — `GET
    /api/admin/console/not-found`.
  - `templates/console/fragments/not_found_report_fragment.html` — card,
    filters, table, pagination, collection-scope note, own JS exposing
    `loadNotFound(days)`.
  - `dashboard.html` — one nav tab button, one pane div with `th:replace`, two
    dispatch lines.
- [x] **Step 2: Tests** — `NotFoundReportServiceTest` (Mockito) at the service
  boundary: sort selection, bot exclusion, internal-referer pattern, paging.
  `NotFoundStatisticsRepositoryTest` (`@DataJpaTest`) runs the aggregation on the
  test database so the JPQL and the referer classification are really executed.
- [x] **Step 3: Rollout / Rollback** — read-only feature, no migration. Revert
  the commits to remove it.

## Validation
- **Commands run:** `./gradlew test` and
  `OPENAI_API_KEY=dummy-key-for-context-load ./gradlew test --rerun-tasks`.
- **Result:** 192 tests. Without a local `OPENAI_API_KEY` the same 10
  context-load failures as before this change occur
  (`AdminMiniAppApiControllerTest`, `FileUploadControllerTest`); with a dummy key
  set the whole suite is green. New tests: `NotFoundReportServiceTest` (6) and
  `NotFoundStatisticsRepositoryTest` (4, runs the JPQL against H2).

## Risks & Rollback
- **Risks:**
  - Internal-referer detection is a `LIKE '%//host/%'` match on the configured
    `app.base-url` host. A foreign referer whose query string embeds
    `//yunseong.dev/` would be misclassified as internal; harmless and rare.
  - No index is added (schema changes are out of scope). The report scans the
    `created_at` window and aggregates; acceptable at ~178k rows. If it ever
    gets slow, a partial index on `(created_at)` where `status_code = 404` is
    the fix.
  - Aggregation is JPQL only, so it stays H2/PostgreSQL portable and is
    executed against H2 in `NotFoundStatisticsRepositoryTest`.
  - The report's own sort / bot / page state is not written into the dashboard
    URL, because `pushState` belongs to `dashboard.html`, which is limited to
    wiring edits here. Only `days` and `tab=notfound` are deep-linkable.
- **Rollback steps:** `git revert` the feature commits; nothing persisted.

## Open Questions
- None.
