# 2026-06-19 — Profile Memo Homepage

- Date: 2026-06-19
- GitHub Issue: #102
- Status: Complete

## Goal

- Render a `/profile` memo written as YAML on the homepage.
- Merge profile identity, description, portrait, and contacts into the existing hero.
- Present projects, awards, education, certificates, and assessments in the selected editorial layout.
- Provide a complete YAML example and an idempotent SQL template that creates the profile memo.
- Validate profile YAML during editing and immediately before persistence.
- Show explicit missing and invalid profile states.

## Non-goals

- Replacing the memo editor with a dedicated profile editor.
- Changing the database schema.
- Changing ordinary Markdown memo editing behavior.
- Redesigning the site-wide navigation or visual system.

## Context / Constraints

- Preserve the current paper, blue, sage, mono-label, square-corner design language.
- Treat memo YAML as untrusted input and map it into explicit immutable view data.
- A missing or invalid profile memo must not break the homepage.
- Keep the SQL template manually executable; do not mutate production data automatically.

## Approach (Checklist)

- [x] **Step 0: Recon** (Inspect controller, memo service, templates, CSS, SQL schema, and tests)
- [x] **Step 1: Implementation** (Remove homepage README, add load states, editor validation endpoint, save guard, and error UI)
- [x] **Step 2: Tests** (Add save rejection and missing/invalid state coverage; run focused and full tests)
- [x] **Step 3: Rollout / Rollback** (Document SQL seed, validation behavior, and fallback state)

## Validation

- **Commands run:** focused profile tests, `./gradlew test`, `./gradlew build`, `git diff --check`
- **Result:** All tests and build passed. Invalid profile YAML is rejected before insert/update; homepage distinguishes missing and invalid states.

## Risks & Rollback

- **Risks:** YAML shape drift, malformed external links, overly dense mobile layout, profile memo accidentally appearing as ordinary content.
- **Rollback steps:** Revert profile parser/controller/template changes and restore the README fragment in `index.html`; no database migration rollback is required.

## Open Questions

- None. Use `/profile` as the canonical memo name and preserve YAML list order as display order.
