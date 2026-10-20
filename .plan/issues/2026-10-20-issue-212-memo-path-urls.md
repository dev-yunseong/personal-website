# 2026-10-20 — Memo path URLs

- Date: 2026-10-20
- GitHub Issue: #212
- Status: In progress

## Goal
Open an exact memo at `/public/blog/{path}`, otherwise use the path as a category filter. The private/draft path is only an example; any nested path is supported.

## Non-goals
No publishing, visual design, sitemap, or canonical URL changes. Existing ID URLs remain available.

## Context / Constraints
Reuse PublicMemoController, MemoService, the existing detail/list templates, and authentication rules. Anonymous private paths return 404. Paths without a memo show the category listing, including empty categories. Root paths show the public memo list; trailing slashes are normalized.

## Approach (Checklist)
- [x] **Step 0: Recon** Inspect memo routes, repository, visibility aspect, and integration test setup.
- [x] **Step 1: Implementation** Add a catch-all path route and public-only optional name lookup; share detail rendering and fall back to existing category listing.
- [ ] **Step 2: Tests** Cover nested/encoded names, private memo/category access, exact memo precedence, category fallback, pagination, root paths, trailing slashes, and existing ID routes.
- [ ] **Step 3: Rollout / Rollback** Publish a separate PR linked to #212. Revert its commit to remove the route.

## Validation
- **Commands to run:** `./gradlew test`; `git diff --check`.
- **Expected output:** All tests pass, private content is never rendered anonymously, unchanged ID routes.
- **Environment:** Local Java is version 11, while the project requires Java 21; use the existing GitHub Actions Java 21 runner if local execution is unavailable.
- **Initial CI:** Only the percent-character case failed because the existing security firewall rejects encoded percent signs (400). Preserve that protection and test it explicitly; Korean, spaces, and plus characters are supported.

## Risks & Rollback
- **Risks:** Path decoding and private visibility must match existing memo semantics. Renaming a memo changes its name-based URL; stable numeric links remain canonical.
- **Rollback steps:** Revert the feature commit; no database migration required.

## Open Questions
- None.
