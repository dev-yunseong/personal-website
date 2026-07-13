# 2026-07-13 — Curator Agent Global Memory

- Date: 2026-07-13
- GitHub Issue: None
- Status: Draft

## Goal

Add curator-agent memory that persists globally across sessions by writing private memos under `/private/curator`, expose a tool the agent can use to create/update those memos, and show the memory behavior on the curator page through hover help.

## Non-goals

Do not redesign the chat interface, change public memo visibility, add new persistence tables, or alter unrelated agent tools.

## Context / Constraints

The project already has memo persistence, private memo paths, Spring AI tool wiring, and a private `/private/...` visibility convention. Changes should reuse those systems and keep memory private by default.

## Approach (Checklist)
- [ ] **Step 0: Recon** (Inspect existing AI service, tools, memo service, and curator page)
- [ ] **Step 1: Implementation** (Add curator memory tool, system prompt instructions, and hover UI)
- [ ] **Step 2: Tests** (Add focused tests where service/tool behavior is observable; run targeted Gradle tests)
- [ ] **Step 3: Rollout / Rollback** (No migration expected; rollback by reverting code changes)

## Validation
- **Commands to run:** `./gradlew test` or focused affected tests
- **Expected output:** Tests pass

## Risks & Rollback
- **Risks:** Agent may over-write memory unexpectedly; tool should constrain writes to `/private/curator`.
- **Rollback steps:** Revert the code changes and remove any created private curator memo if needed.

## Open Questions
- None; assume `/private/curator` is the canonical global memory namespace.
