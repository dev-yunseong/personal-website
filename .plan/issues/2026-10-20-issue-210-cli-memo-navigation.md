# 2026-10-20 — CLI memo navigation with attached drafts

- Date: 2026-10-20
- GitHub Issue: #210
- Status: Completed

## Goal
- Browse complete or scoped memo trees and direct-category lists.
- Attach draft indicators to target memo paths, including draft-only new memos.
- Read draft bodies by public memo ID or target path with DRAFT_TOKEN.

## Non-goals
Console token issuance, browsing arbitrary private memos, direct CLI publication.

## Context / Constraints
Keep the existing shell/curl CLI and paginated public API. Drafts remain stored under `/private/draft/`.
Anonymous reads expose only public content. Token access adds only draft-aware navigation and draft reads.

## Approach (Checklist)
- [x] **Step 0: Recon** Inspect public API, memo paths, draft security and CLI.
- [x] **Step 1: Implementation** Query metadata without bodies, merge drafts by target path, render sorted trees; add scoped list and draft read routes.
- [x] **Step 2: Tests** Scope boundaries, Unicode, literal wildcard names, public privacy, token checks, draft-only entries, CLI argument forwarding.
- [x] **Step 3: Rollout / Rollback** Update docs and existing PR. No migration; revert navigation changes to roll back.

## Validation
- **Results:** Focused tests and full Gradle suite passed: 658 tests, 0 failures/errors/skips. `sh -n bin/website` and `git diff --check` passed.
- **Verified:** Scoped tree/list/read, English `[draft]` / `[draft only]` markers, token access and isolation of unrelated private content. Full suite: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 sh ./gradlew test --offline --no-watch-fs --console=plain` (WSL).

## Risks & Rollback
- **Risks:** Draft token scope expands to draft reads. Tree construction loads scoped metadata, never bodies; very large catalogs still require memory proportional to their paths. Use segment boundaries, not wildcard matching.
- **Rollback steps:** Revert this addition; draft storage and console approvals remain unchanged.

## Open Questions
- None. Token issuance from console is a separate follow-up, not part of this change.
