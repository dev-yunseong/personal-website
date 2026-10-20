# 2026-10-20 — Memo path URLs

- Date: 2026-10-20
- GitHub Issue: #212
- Status: In progress

## Goal
Open a memo by name at `/public/blog/{memo path}`, including authenticated access to `/public/blog/private/draft`.

## Non-goals
No publishing, visual design, sitemap, or canonical URL changes. Existing ID URLs remain available.

## Context / Constraints
Reuse PublicMemoController, MemoService, the existing detail template, and authentication rules. Anonymous private lookups must return the same 404 as missing names.

## Approach (Checklist)
- [x] **Step 0: Recon** Inspect memo routes, repository, visibility aspect, and integration test setup.
- [x] **Step 1: Implementation** Add a catch-all path route and a public-only name lookup; share detail rendering between ID and name routes.
- [ ] **Step 2: Tests** Cover nested and encoded names, anonymous/private access, authenticated private access, missing names, and existing ID routes.
- [ ] **Step 3: Rollout / Rollback** Publish a separate PR linked to #212. Revert its commit to remove the route.

## Validation
- **Commands to run:** `./gradlew test`; `git diff --check`.
- **Expected output:** All tests pass, private content is never rendered anonymously, unchanged ID routes.
- **Environment:** Local Java is version 11, while the project requires Java 21; use the existing GitHub Actions Java 21 runner if local execution is unavailable.

## Risks & Rollback
- **Risks:** Path decoding and private visibility must match existing memo semantics. Renaming a memo changes its name-based URL; stable numeric links remain canonical.
- **Rollback steps:** Revert the feature commit; no database migration required.

## Open Questions
- None.
