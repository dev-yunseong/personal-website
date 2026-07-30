# 2026-07-30 — Add traffic heatmap and device/browser distribution

- Date: 2026-07-30
- GitHub Issue: #117
- Status: Draft

## Goal

- 7 x 24 day-of-week by hour traffic heatmap for the console dashboard.
- Device class / browser / OS distribution parsed from `user_agent` at query
  time, unknown values collected in an explicit `Unknown` bucket.
- New `Distribution` tab honouring the existing `days` period filter plus a
  bot-inclusion toggle.
- State the heatmap axis time zone in the UI.

## Non-goals

- Screen resolution, language, or any client-side instrumentation.
- Own bot detection: `BotDetector` / `is_bot` from #112 is the source of truth.
- Schema, entity, `RequestLoggingFilter`, or `recordRequest` changes.
- Status-code filtering on this tab (not in the acceptance criteria).

## Context / Constraints

- Time zone basis: `created_at` is PostgreSQL `TIMESTAMP` (no zone), written by
  Hibernate `@CreationTimestamp` from a `LocalDateTime`, i.e. wall-clock time in
  the JVM default zone, and read back unconverted. No `TZ`, `spring.jackson`, or
  `hibernate.jdbc.time_zone` setting exists anywhere in the repository, so the
  zone is whatever the runtime provides (Docker `eclipse-temurin` defaults to
  UTC). Day-of-week and hour are therefore derived from the stored wall clock and
  the UI labels the axis with `ZoneId.systemDefault()`, resolved at runtime, so
  the label always matches the zone the values were written in.
- Bucketing folds `trunc(createdAt, HOUR)` rows (already used by the existing
  hourly timeline query) in Java via `java.time.DayOfWeek`. This keeps the
  day-of-week numbering unambiguous instead of relying on dialect-specific
  `extract(day of week)` origins (PostgreSQL 0 = Sunday, H2 1 = Sunday).
- Aggregation stays in SQL: at most `24 * days` hour buckets and one row per
  distinct User-Agent, never whole rows. ~178k rows in production.
- No new index: both queries range-scan `created_at`, covered by the existing
  `idx_request_statistics_created_at`.
- No new dependency. Heatmap renders as a CSS grid; the Chart.js matrix plugin is
  out and a scatter-with-square-points hack is more code than the grid.
- Parallel agents own #113–#116 and #118. Own repository / service / domain /
  controller / fragment files only; `dashboard.html` gets one nav button, one
  pane, and the tab dispatch lines.

## Approach (Checklist)
- [x] **Step 0: Recon** — read `RequestStatisticsRepository`,
      `RequestStatisticsService`, `ConsoleApiController`, `UriStat`,
      `TimelineStat`, `RequestStatistics`, `BotDetector`, `WebSecurityConfig`,
      `console/dashboard.html`, `sql/schema.sql`, existing tests. Baseline
      `./gradlew test`: 10 pre-existing failures, all `IllegalStateException`
      context-load errors from a missing `OPENAI_API_KEY` in this sandbox
      (`AdminMiniAppApiControllerTest`, `FileUploadControllerTest`).
- [ ] **Step 1: Implementation**
  - `manage/domain/UserAgentProfile.java`: record `(deviceClass, browser,
    operatingSystem)` with `static of(String userAgent)`; finite ordered rule
    set, `Unknown` fallback.
  - `manage/domain/TrafficSlice.java`: record `(name, count)`.
  - `manage/domain/TrafficDistribution.java`: record `(timeZone,
    hourOfWeekCounts, devices, browsers, operatingSystems)`.
  - `manage/repository/TrafficDistributionRepository.java`: two queries, hour
    buckets and per-User-Agent counts, both `AND (r.isBot = false OR
    :includeBots = true)`.
  - `manage/service/TrafficDistributionService.java`: fold buckets into
    `long[7][24]`, parse distinct User-Agents, sort slices by count desc.
  - `manage/controller/TrafficDistributionApiController.java`:
    `GET /api/admin/console/distribution?days&includeBots`.
  - `templates/console/fragments/distribution.html`: CSS-grid heatmap, three
    distribution bar lists, bot toggle, time-zone note, `window.loadDistribution`.
  - `templates/console/dashboard.html`: nav button, pane, two dispatch lines.
- [ ] **Step 2: Tests**
  - `UserAgentProfileTest`: Chrome/Firefox/Safari/Edge on Windows and macOS,
    iPhone, iPad, Android phone, Android tablet, empty, null, unrecognised.
  - `TrafficDistributionServiceTest`: hour buckets fold onto the right
    day-of-week/hour cell, counts merge per parsed dimension, slices ordered by
    count, `includeBots` reaches the repository.
- [ ] **Step 3: Rollout / Rollback** — read-only feature, no migration. Revert
      the commits to remove it.

## Validation
- **Commands to run:** `./gradlew test`
- **Expected output:** new tests green, failure count unchanged from the
  baseline (10 environment-caused failures).

## Risks & Rollback
- **Risks:** the two new HQL queries cannot be executed in this sandbox (every
  Spring-context test needs secrets that are absent), so `trunc(createdAt, HOUR)`
  and the `:includeBots` predicate are verified by reuse and review only —
  `trunc(..., HOUR)` is already in `findHourlyRequestCounts` on the same entity
  and dialect. If the server zone is UTC while the audience is Korean, the
  heatmap is shifted 9 hours; the UI states the zone so the reading is not wrong,
  only zone-relative. Rows written before a runtime zone change mix two wall
  clocks.
- **Rollback steps:** `git revert` the commits; nothing persistent changes.

## Open Questions
- Should the axis be converted to a fixed display zone (`Asia/Seoul`) instead of
  the server zone? Deferred: it needs a configured display zone nobody asked
  for, and the label keeps the current output honest.
