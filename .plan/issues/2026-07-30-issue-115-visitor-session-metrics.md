# 2026-07-30 — Visitor and session metrics from request logs

- Date: 2026-07-30
- GitHub Issue: #115
- Status: Implemented

## Goal

- Derive visitor and session metrics from `request_statistics` at query time, with
  no new column, entity, or filter change.
- Visitor key is `ip` + `user_agent`; a session breaks after 30 minutes of inactivity.
- Expose unique visitors, session count, pages per session, single-page-session
  (bounce) rate, median session length, returning-visitor rate, and top entry /
  exit pages.
- Add a Visitors tab to the console dashboard honouring the `days` filter and a
  bot-exclusion toggle.
- Keep a wide period from loading the whole table into the application.

## Non-goals

- Cookie / localStorage visitor identifiers.
- Client-side instrumentation (scroll depth, dwell time).
- Sankey path visualisation.
- Touching the entity, `schema.sql`, `RequestLoggingFilter`, or `recordRequest`.

## Context / Constraints

Branch `enhancement/115` is based on `enhancement/112` (commit `1c6ba37`), which
added `is_bot` / `duration_ms` and widened collection to `/`, `/public/**`,
`/api/public/**`. The homepage now appears in the data, so entry-page analysis is
possible.

Production holds ~178k rows since 2025-12-17 with ~4.2k distinct IPs.
`idx_request_statistics_created_at` already exists and serves the period range
scan, so no new index is needed.

Parallel agents own `RequestStatisticsRepository`, `RequestStatisticsService`,
`ConsoleApiController`, and most of `dashboard.html`. This work therefore lands in
new files, and `dashboard.html` gets only tab wiring.

Sessionisation runs entirely in SQL (`LAG` + running `SUM` window functions), so
the application receives one summary row plus one row per entry/exit URI. Tests
run on H2, so the SQL must stay within the intersection of PostgreSQL and H2 2.x:
window functions, `INTERVAL '30' MINUTE`, `PERCENTILE_CONT`, and
`EXTRACT(EPOCH FROM ...)` are all expected to work on both — to be verified by an
H2-backed repository test rather than assumed.

The visitor key stays inside the query. No aggregate needs to report visitor
identity, so `ip` never appears in a select list and never leaves the database.

## Approach (Checklist)
- [x] **Step 0: Recon** — read `RequestStatisticsRepository`, `RequestStatisticsService`,
      `ConsoleApiController`, `UriStat`, `console/dashboard.html`, `schema.sql`,
      test conventions.
- [x] **Step 1: Implementation**
      - `manage/domain/VisitorMetrics.java`: result record (reuses `UriStat` for
        entry/exit lists).
      - `manage/repository/VisitorSessionRepository.java`: two native queries —
        session summary, entry/exit page counts.
      - `manage/service/VisitorSessionService.java`: row mapping plus ratio
        arithmetic with empty-period guards.
      - `manage/controller/VisitorApiController.java`: `GET /api/admin/console/visitors`.
      - `templates/console/fragments/visitors.html`: metric cards, approximation
        notice, entry/exit tables, `loadVisitors(days)`.
      - `console/dashboard.html`: nav tab button, fragment include, tab dispatch.
- [x] **Step 2: Tests**
      - `VisitorSessionRepositoryTest` (`@DataJpaTest`, H2): 30-minute boundary on
        both sides, one visitor with multiple sessions, multiple visitors at the
        same instant, bot exclusion, entry/exit attribution.
      - `VisitorSessionServiceTest`: ratio arithmetic and empty-period behaviour.
      - `VisitorsFragmentTemplateTest`: fragment selector and dashboard include.
- [x] **Step 3: Rollout / Rollback** — read-only feature behind existing
      `/admin/**` authentication. Rollback is `git revert`; no migration.

## Validation
- **Commands to run:** `./gradlew test`
- **Result:** BUILD SUCCESSFUL, 194 tests, 0 failures (baseline 182). The suite
  needs `OPENAI_API_KEY` set to any value; without it 10 pre-existing context-load
  failures occur in `FileUploadControllerTest` and `AdminMiniAppApiControllerTest`
  on the base branch too.
- **Dialect and cost check (throwaway PostgreSQL 16 container, not part of the
  suite):** both statements ran unchanged and agreed with the H2 results. On a
  seeded 178k-row / 4200-IP table: 7-day window 24 ms per query using
  `idx_request_statistics_created_at`; full 225-day window 443 ms and 526 ms with a
  sequential scan and an external merge sort (~7-9 MB spill at the default 4 MB
  `work_mem`). Rows returned to the application: 1 summary row and 240 page rows.

## Risks & Rollback
- **Risks:**
  - IP+UA identity is an approximation: one NAT collapses several users into one
    visitor, a rotating mobile IP splits one user into several. Must be labelled
    approximate in the UI.
  - Window-function SQL verified on H2 and read as portable, but only the
    PostgreSQL plan matters in production and it cannot be executed here.
  - A very wide period scans every matching row inside PostgreSQL. Sorting for the
    window partitions is the cost driver; acceptable at the current table size.
- **Rollback steps:** `git revert` the commits; nothing persisted, nothing migrated.

## Open Questions
- None blocking.
