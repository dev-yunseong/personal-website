# 2026-08-12 — Agent Briefing Intake And Digest Page

- Date: 2026-08-12
- GitHub Issue: #163
- Status: Implemented

## Goal

Receive the daily news/jobs briefings an external agent (hermes) generates, store
them, and read a whole day on one page.

Two properties drive every decision below:

- **Extensibility.** A kind (`news`, `jobs`, whatever comes next) is only a path
  segment. A new kind means posting once under a new name — no registry table,
  no server change.
- **Uniformity.** However many kinds exist, there is one path shape, one post
  format, one API, one CLI.

## Non-goals

- Generating, collecting, or scheduling briefings. That is hermes' job.
- New entity, table, or migration.
- A draft/approval UI. Editing or soft-deleting the memo is enough to recover.

## Context / Constraints

- A briefing is a memo at `/private/briefing/<kind>/<yyyy-MM-dd>`. The
  `/private` prefix is the blocking mechanism itself, so the blog list, the
  category tree, search, and the sitemap stay clean with no filter code.
- Storage is private; `/briefing` is public. A briefing never sits between blog
  posts, but the reading is still shareable.
- The digest page renders memos it fetched *without* the visibility filter, so
  the query prefix is the only thing keeping private memos in. It must stay
  pinned to `/private/briefing/`.
- `fragments/mermaid_katex_parser.html` resolves exactly one `#markdown-content`
  element, so a whole day has to render inside that single container.
- Dedicated token auth (`X-Briefing-Token`), separate from the admin account: a
  leak costs one env var and never reaches `/admin/**`.
- Plain text in and out, no JSON, so the CLI is one `curl` and a markdown body
  needs no shell escaping.

## Approach (Checklist)

- [x] **Step 0: Recon** — `MemoService.upsertMemo`, `Memo.PRIVATE_PREFIX`,
      `ContentVisibilityAspect`, `WebSecurityConfig`, `memo-view.css`,
      `mermaid_katex_parser.html`.
- [x] **Step 1: Implementation**
  - `briefing/service/BriefingService.java` — kind/date/body validation, the
    uniform wrapper (`# title` + `kind · date · 에이전트 작성` + body), then
    `MemoService.upsertMemo`. Reads: `findLatest`, `listKinds`, `recentDates`,
    `findByDate`.
  - `briefing/controller/AgentBriefingApiController.java` — `POST
    /api/agent/briefings/{kind}`, `GET /{kind}/latest`, `GET /kinds`, all
    `text/plain`.
  - `briefing/controller/BriefingController.java` — `GET /briefing`, falling
    back to the latest date.
  - `global/config/AgentApiSecurityConfig.java` — `@Order(1)` chain over
    `/api/agent/**`, stateless, CSRF off, constant-time token compare, fail
    closed when unset.
  - `templates/briefing.html` + `static/css/briefing.css`.
  - `bin/briefing`, `application.yml`, `.env.example`, `SitemapService`, header
    nav, README.
- [x] **Step 2: Tests** — `BriefingServiceTest`, `AgentBriefingApiControllerTest`,
      `BriefingControllerTest`, `BriefingTemplateTest`, `BriefingTokenFilterTest`.
- [x] **Step 3: Rollout** — set `BRIEFING_TOKEN` in the deployment environment.
      Until it is set the intake API answers 401 and nothing else changes.

## Validation

- **Commands to run:** `./gradlew test`
- **Expected output:** whole suite green.
- **Manual:** publish `news` and `jobs` for one day through `bin/briefing`,
  confirm `/briefing` expands both bodies, that re-publishing the same day
  updates rather than appends, that `?date=` navigates, that a signed-out
  `/public/memos` list, category tree, and search do not show them, and that a
  request without the token is 401.

## Risks & Rollback

- **Risks:**
  - The digest page bypasses the visibility filter. Widening the query prefix
    beyond `/private/briefing/` would leak private memos. Guarded by
    `BriefingService.BRIEFING_ROOT` and a test.
  - The kind is a path segment, so an unvalidated kind could escape the prefix.
    Guarded by the `[a-z0-9][a-z0-9-]{0,31}` pattern.
  - A leaked token lets anyone publish briefings. It cannot reach `/admin/**`,
    and rotating one env var revokes it.
- **Rollback steps:** unset `BRIEFING_TOKEN` to close intake immediately;
  `git revert` the commit to remove the page. Stored briefings are ordinary
  memos and survive either way.

## Open Questions

- None.
