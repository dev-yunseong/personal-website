# 2026-10-20 — CLI memo drafts and console approval

- Date: 2026-10-20
- GitHub Issue: #210
- Status: Implemented and verified

## Goal
- Submit Markdown through the existing CLI into `/private/draft/<target>`.
- Review new content and git-style diffs in the admin console.
- Approve with PUT semantics, preserving existing target IDs.
- Reject approvals if either reviewed document changed.

## Non-goals
Direct CLI publishing, attachments, bulk changes, new persistence tables.

## Context / Constraints
Memo names are unique in production. Private prefixes already exclude drafts from public surfaces.
Use a separate draft-only token; retain browser authentication and CSRF for approval.
The generic caveman skill was not found in installed skills; keep communication concise.

## Approach (Checklist)
- [x] **Step 0: Recon** Inspect memo storage, public filtering, CLI, console, security.
- [x] **Step 1: Implementation** Draft service, intake endpoint/security, console list/review/PUT, CLI command.
- [x] **Step 2: Tests** Lifecycle, review conflicts, privacy/security, line diff and CLI payload tests.
- [x] **Step 3: Rollout / Rollback** Documented DRAFT_TOKEN configuration; no migration; revert feature to disable. Production deployment is not included.

## Validation
- **Commands to run:** Gradle wrapper focused tests, full test suite; manual console check when local runtime permits.
- **Expected output:** Passing tests; drafts remain private until approved; changed target requires re-review.
- **Results:** JDK 21 Gradle wrapper `test --offline --console=plain`: 646 tests passed, zero failures/errors/skips. Existing briefing script CRLF was normalized locally for shell tests; its committed content is unchanged.
- **Manual verification:** Local H2 server: CLI submission, new publication, replacement retaining memo ID, stale review rejection and successful re-review. Windows Git Bash path conversion handled. `node --check` and `git diff --check` passed.
- **Screenshot:** [Existing memo diff review](2026-10-20-issue-210-draft-review.png).

## Risks & Rollback
- **Risks:** Concurrent changes must be checked under a row lock. Target insert collisions must roll back without consuming the draft. Render proposed text escaped, not executable HTML.
- **Rollback steps:** Revert feature; private drafts remain ordinary private memos.

## Open Questions
- None. Treat private/draft as the existing memo namespace, and PUT as target-path create-or-replace.
