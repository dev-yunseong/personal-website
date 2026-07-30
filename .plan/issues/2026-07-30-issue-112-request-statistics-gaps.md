# 2026-07-30 — Fix request statistics collection gaps and add bot/duration columns

- Date: 2026-07-30
- GitHub Issue: #112
- Status: Draft

## Goal

- Widen collection from `/public/**` only to the full public surface: `/` exact,
  `/public/**`, `/api/public/**`.
- Persist `is_bot`, decided from the User-Agent at insert time.
- Persist `duration_ms`, already computed and discarded by `RequestLoggingFilter`.
- Keep bot classification in one tested place (`BotDetector`).
- Provide a backfill for existing rows.

## Non-goals

- `country_code` / IP geolocation.
- Dashboard UI changes (`templates/console/dashboard.html` untouched).
- Read-side aggregation queries, rollups, or changes to the in-memory queue and
  5-minute flush. Follow-up issues #113–#117 own those, read-only.

## Context / Constraints

- `RequestStatisticsService.recordRequest` currently gates on
  `uri.startsWith("/public/")`, so the homepage (`MainController` `/`) and
  `/api/public/search/suggestions` are never recorded.
- Public controller mappings verified: `/`, `/public/memos`, `/public/projects`,
  `/public/apps`, `/public/search`, `/public/chat`, `/api/public/search/*`,
  `/api/public/chat`. Admin routes live under `/admin/**` and `/api/admin/**`.
  A pure whitelist therefore excludes static assets and admin routes by
  construction — no separate deny list is needed.
- `ddl-auto: none`; `src/main/resources/sql/schema.sql` is an append-only,
  manually applied PostgreSQL migration log. Tests run on H2 and never execute
  it.
- Bot decision runs on the request path: one precompiled regex, no external
  calls.

## Approach (Checklist)
- [x] **Step 0: Recon** — read filter, entity, service, repository,
      `MainController`, all controller mappings, `sql/schema.sql`, existing
      service test. Baseline `./gradlew test` green.
- [ ] **Step 1: Implementation**
  - `manage/domain/BotDetector.java`: `public static boolean isBot(String userAgent)`,
    one case-insensitive regex; null/blank User-Agent counts as a bot.
  - `manage/domain/RequestStatistics.java`: add `isBot` (`is_bot`, not null) and
    `durationMs` (`duration_ms`, nullable); keep existing constructors working.
  - `manage/service/RequestStatisticsService.java`: whitelist check, collapse the
    `recordRequest` overloads into one that also takes `durationMs`, set `isBot`
    via `BotDetector`.
  - `global/config/RequestLoggingFilter.java`: pass the duration it already
    computes.
  - `sql/schema.sql`: `ALTER TABLE ... ADD COLUMN` for both columns plus a
    backfill `UPDATE` for existing rows.
- [ ] **Step 2: Tests**
  - New `BotDetectorTest`: Googlebot, GPTBot, ClaudeBot, bingbot, curl, wget,
    python-requests, Chrome, Safari, mobile Safari, null, empty.
  - Extend `RequestStatisticsServiceTest`: `/` and `/api/public/**` recorded,
    static assets and admin routes skipped, `isBot` and `durationMs` persisted.
- [ ] **Step 3: Rollout / Rollback**
  - Apply the `ALTER TABLE` statements before deploying; defaults keep existing
    rows and every existing query valid.

## Validation
- **Commands to run:** `./gradlew test`
- **Expected output:** `BUILD SUCCESSFUL`, no pre-existing failures introduced.

## Risks & Rollback
- **Risks:** total request counts jump once the whitelist widens, so pre/post
  periods are not comparable — note it in the release notes. Substring-based bot
  matching (`bot`) can misclassify an exotic device name; the cost is one row
  labelled bot, and the regex lives in one file.
- **Rollback steps:** `git revert` the commits. The added columns are additive
  and nullable/defaulted, so leaving them in place breaks nothing.

## Open Questions
- Blank/absent User-Agent is classified as a bot (real browsers always send
  one). Revisit if legitimate traffic turns up without the header.
