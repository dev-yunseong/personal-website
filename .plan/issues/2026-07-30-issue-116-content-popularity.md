# 2026-07-30 — Content Popularity With Memo Titles And Categories

- Date: 2026-07-30
- GitHub Issue: #116
- Status: Draft

## Goal

- Resolve memo detail URIs (`/public/memos/{id}`) to memo title and category in the console.
- Aggregate traffic per category (memo path prefix).
- Distinguish fresh reaction from evergreen content (published over 30 days ago, still ranking).
- Add a Content tab to the console dashboard honouring the `days` filter and a bot-exclusion toggle.
- Keep unresolvable rows in the aggregate by falling back to the raw URI.

## Non-goals

- Memo schema change, view-counter column, new entity or filter change.
- Recommendation or ranking algorithms.
- Any change to `RequestStatistics`, `RequestLoggingFilter`, `recordRequest`, or `schema.sql`.

## Context / Constraints

- Based on `enhancement/112` (bot flag, `duration_ms`, widened collection surface); base tip `1c6ba37`.
- Five sibling agents work in parallel: new repository/service/controller/fragment files only.
  `dashboard.html` gets nav tab + pane include + tab dispatch lines and nothing else.
- No new dependencies. Reuse the Chart.js 4.x already loaded by `dashboard.html`.
- Aggregate in SQL. `request_statistics` holds ~178k rows and grows.
- Statistics must not bypass `FilterVisibleContent` / `ContentVisibilityAspect`.
- Tests run on H2; production on PostgreSQL. Keep SQL portable (JPQL only).

## Approach (Checklist)

- [ ] **Step 0: Recon** (`RequestStatisticsRepository/Service`, `ConsoleApiController`, `UriStat`,
      `dashboard.html`, `PublicMemoController`, `Memo`, visibility aspect, `WebSecurityConfig`, `schema.sql`)
- [ ] **Step 1: Implementation**
  - `manage/domain/MemoUriParser` — static `extractMemoId(String uri)`; handles query string,
    fragment, trailing slash, sub-paths; rejects the list path `/public/memos`.
  - `manage/domain/MemoTrafficStat`, `manage/domain/CategoryTrafficStat` — response records.
  - `manage/repository/ContentPopularityRepository` — one JPQL aggregate grouped by `uri`,
    prefix-filtered to `/public/memos/%`, with `days` window and optional `isBot = false`.
  - `manage/service/ContentPopularityService` — normalise URI variants, merge counts, one
    `findAllById` join to `Memo`, derive category totals and evergreen flags.
  - `manage/controller/ContentPopularityApiController` — `GET /api/admin/console/content`.
  - `templates/console/fragments/content.html` — markup + JS + Chart.js doughnut, exposes
    `window.loadContentStats(days)`.
  - `dashboard.html` — nav tab button, pane include, two dispatch lines.
- [ ] **Step 2: Tests** (`MemoUriParserTest` for parsing edges; `ContentPopularityServiceTest` for
      resolution, private/deleted fallback, category totals, evergreen split, bot exclusion)
- [ ] **Step 3: Rollout / Rollback** (read-only feature, no migration; revert the commits)

## Visibility Decision

- Response exposes only: request counts, memo id, memo title (last `name` segment), category
  (`name` prefix), and `createdAt`. Never memo `content`.
- Private and deleted memos (`name` starting `/private`) are treated exactly like a join miss:
  the aggregate row is kept with its raw URI and no memo metadata. One rule covers deleted,
  missing, and private memos, so the statistics endpoint cannot surface a non-public memo name.
- Endpoint lives under `/api/admin/**`, already `authenticated()` in `WebSecurityConfig`.

## Validation

- **Commands to run:** `./gradlew test`
- **Expected output:** BUILD SUCCESSFUL, new parser/service tests passing.

## Risks & Rollback

- **Risks:** the group-by returns one row per distinct memo-detail URI, so query-string variants
  inflate the intermediate result set (bounded, but not constant); no `text_pattern_ops` index
  exists for the `LIKE '/public/memos/%'` prefix scan and schema changes are out of scope.
- **Rollback steps:** `git revert` the commits; nothing persisted, no migration.

## Open Questions

- None. Category is `Memo.getPath()`; evergreen threshold fixed at 30 days per the issue.
