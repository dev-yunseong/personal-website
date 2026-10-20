# 2026-10-20 — Inline help tooltips

- Date: 2026-10-20
- GitHub Issue: #214 (enhancement/214)
- Status: Implemented; runtime validation limited by environment

## Goal
- Replace persistent explanatory copy with circular information buttons.
- Cover draft review, console analytics, memo/mini-app form hints, and chat usage guidance.
- Keep feedback, warnings and empty states visible.

## Non-goals
- Change approval, analytics, or persistence behavior.

## Context / Constraints
- Preserve Thymeleaf and existing Bootstrap styling; support dynamic console content.

## Approach (Checklist)
- [x] **Step 0: Recon** Inspect templates and existing Bootstrap bundle.
- [x] **Step 1: Implementation** Shared delegated Bootstrap tooltips and inline help buttons.
- [x] **Step 2: Tests** Shared script and 14 inline console scripts parse. Gradle tests blocked: Java/JAVA_HOME unavailable. Browser preview blocked: local URL connection timed out.
- [x] **Step 3: Rollout / Rollback** Ship static assets/templates together; revert these changes to roll back. Not deployed.

## Validation
- CI follow-up: update two legacy template assertions to verify tooltip guidance and conditional filter help; rerun the full suite in GitHub Actions.
- **Commands to run:** Gradle Wrapper test; node --check.
- **Expected output:** Tests pass; scripts parse; supplementary text appears on hover/focus.

## Risks & Rollback
- **Risks:** Touch/keyboard access and dynamically replaced content need shared handling.
- **Rollback steps:** Revert template and shared asset changes.

## Open Questions
- None.
