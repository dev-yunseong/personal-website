# 2026-10-20 — Stabilize sitemap projection test

- Date: 2026-10-20
- GitHub Issue: None; existing PR #209 follow-up
- Status: In progress

## Goal
Replace environment-dependent heap comparisons with deterministic coverage of the real sitemap query.

## Non-goals
No production query or sitemap behavior changes; no performance claims from simulated allocations.

## Context / Constraints
PR #209 failed at a JVM heap comparison. Concurrent bot commit ab29ad4 replaced it with synthetic output comparisons and also removed unrelated main-branch features.

## Approach (Checklist)
- [x] **Step 0: Recon** Inspect CI failure, current PR, repository and existing test conventions.
- [ ] **Step 1: Implementation** Replace SitemapBenchmarkTest with MemoRepositorySitemapTest.
- [ ] **Step 2: Tests** Verify projection values, public filtering, empty results, one query and no entity loading; run full suite.
- [ ] **Step 3: Rollout / Rollback** Push a normal commit to the existing PR branch; revert that commit if needed.

## Validation
- **Commands to run:** `./gradlew test --tests '*MemoRepositorySitemapTest'`; `./gradlew test`; `git diff --check`.
- **Expected output:** Passing tests; no whitespace errors.

## Risks & Rollback
- **Risks:** Hibernate statistics must be cleared after fixture persistence; unrelated concurrent bot deletions need user direction.
- **Rollback steps:** Revert the test correction commit.

## Open Questions
- Restore the unrelated feature deletions introduced by the concurrent bot commit? User asked asynchronously.
