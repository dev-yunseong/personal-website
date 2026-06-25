# 2026-06-25 — Homepage Memos List

- Date: 2026-06-25
- GitHub Issue: None
- Status: Implemented

## Goal

Show a compact recent memos list on the main page so visitors can move from the profile/homepage into current writing without first opening the full blog page.

## Non-goals

- Do not redesign the homepage visual language.
- Do not change memo creation, editing, markdown rendering, or search behavior.
- Do not add a new table, migration, cache, or publication flag unless later requirements prove it necessary.
- Do not expose admin-only actions in the public homepage section.

## Context / Constraints

- `MainController` currently loads profile data only and returns `index`.
- `index.html` renders `fragments/home_hero_fragment :: homeHero`, then a search area.
- Public memo list behavior already exists under `/public/memos`.
- `MemoService#getMemos(Pageable)` already returns memo pages and can be reused for a small newest list.
- `MemoRepository` already supports paged `findAll`.
- Existing homepage uses editorial resume-style sections in `home.css`; the memo section should match that structure instead of introducing card-heavy marketing UI.
- Profile memo `/profile` should not be shown as a normal writing item on the homepage if possible.

## Approach (Checklist)

- [x] **Step 0: Recon** (Inspect existing code, locate files)
  - [x] Confirm current memo ordering from `/public/memos` and whether it should be `updatedAt DESC` or `createdAt DESC`.
  - [x] Confirm whether special memos such as `/profile` should be excluded from public lists generally or only from the homepage.
  - [x] Check existing homepage tests: `MainControllerTest` and `HomeProfileTemplateTest`.
- [x] **Step 1: Implementation** (Code changes, file paths)
  - [x] Add a small read-only service method in `src/main/java/dev/yunseong/website/blog/service/MemoService.java`, likely `getRecentMemos(int limit)`.
  - [x] Back it with repository/query behavior that sorts by `updatedAt DESC`, limits result size, and excludes `/profile`.
  - [x] Inject `MemoService` into `src/main/java/dev/yunseong/website/global/controller/MainController.java`.
  - [x] Add `recentMemos` model attribute in `MainController#index`.
  - [x] Add a homepage section in `src/main/resources/templates/index.html` or a new fragment under `templates/fragments`, reusing existing section rhythm.
  - [x] Render each memo with title/path/date and link to `/public/memos/{id}`.
  - [x] Add a clear empty state only if there are no public memos.
  - [x] Add a "View all" link to `/public/memos`.
  - [x] Style in `src/main/resources/static/css/home.css`, using the current border/grid/mono-label language.
- [x] **Step 2: Tests** (Unit tests, manual verification steps)
  - [x] Update/add `MainControllerTest` to verify `recentMemos` is added.
  - [x] Add/update `MemoServiceTest` for sort, limit, and `/profile` exclusion if repository behavior is changed.
  - [x] Update `HomeProfileTemplateTest` or add template coverage for section render and empty state.
  - [x] Run focused tests first, then `./gradlew test`.
  - [x] Manually check homepage at desktop and mobile widths if CSS/template changed.
- [x] **Step 3: Rollout / Rollback** (Feature flags, migration steps)
  - [x] No migration expected.
  - [x] Rollback is one revert of controller/service/template/CSS changes.
  - [x] If production data has many private-looking memo paths, defer until visibility rule is defined.

## Validation

- **Commands to run:**
  - `./gradlew test`
  - Optional focused runs if available through Gradle test filters:
    - `./gradlew test --tests dev.yunseong.website.global.controller.MainControllerTest`
    - `./gradlew test --tests dev.yunseong.website.global.controller.HomeProfileTemplateTest`
    - `./gradlew test --tests dev.yunseong.website.blog.service.MemoServiceTest`
- **Expected output:**
  - Tests pass.
  - Homepage renders profile, recent memos section, search area, and no duplicate `/profile` memo item.

## Risks & Rollback

- **Risks:**
  - Memo ordering may differ from current public list if `Pageable` sort is ignored or column naming differs.
  - `/profile` exclusion could diverge between homepage and `/public/memos` unless scoped intentionally.
  - Homepage may become too dense on mobile if memo metadata is not compact.
  - Showing latest updated memos can surface old posts after minor edits; created-date ordering may better match "new memos" wording.
- **Rollback steps:** Revert the commit that adds `recentMemos` service/controller/template/CSS changes.

## Open Questions

- Resolved: sort by `updatedAt DESC`.
- Resolved: show 3 items.
- Resolved: exclude only `/profile`.
- Resolved: place the section above the search bar so recent writing is visible before secondary search.
