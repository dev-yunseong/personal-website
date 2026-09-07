# 2026-08-15 — Public memo API and website CLI

- Date: 2026-08-15
- GitHub Issue: #173
- Status: Implemented

## Goal

Provide a stable, anonymous, read-only API and `bin/website` CLI for listing changed public memos and reading one public memo as unchanged Markdown, so `llm-wiki` can ingest public content without HTML scraping.

## Non-goals

- Private/deleted memo access or memo mutation.
- Login/session automation.
- Changes in the `llm-wiki` repository.
- Replacing or changing `bin/briefing`.

## Context / Constraints

- Public visibility already lives in `MemoRepository.findPublic*` and `MemoService.getPublic*`; repository/service remain the sole visibility boundary. Controller and CLI must not duplicate private/deleted checks.
- Public and missing memo IDs must both produce 404 without confirming private existence.
- API contract:
  - `GET /api/public/memos?updatedAfter=<timestamp>&page=<n>&limit=<n>`
  - Explicit response record: `{ "items": [{ "id", "name", "updatedAt" }], "page", "limit", "hasNext" }`.
  - Never serialize `Memo`, relationships, content, or Spring `Page` in list responses.
  - `page` is zero-based, default `0`, minimum `0`.
  - `limit` defaults to `100`, accepted range `1..100`.
  - `updatedAfter` is optional, strict-exclusive (`updatedAt > value`), ISO local date-time without offset, for example `2026-08-15T13:30:00`; this matches existing `LocalDateTime` persistence and is server-local time.
  - Invalid timestamp/page/limit returns HTTP 400; error-body shape is not a stable API contract.
  - List ordering is always `updatedAt ASC, id ASC`, including when multiple rows share a timestamp.
  - `GET /api/public/memos/{id}/content` returns `text/markdown; charset=UTF-8`; missing/private/deleted IDs return the same 404.
- CLI contract:
  - `bin/website memo list [--updated-after <timestamp>] [--page <n>] [--limit <n>]`
  - `bin/website memo read <id>`
  - `WEBSITE_URL` defaults to `https://yunseong.dev`.
  - List prints API JSON unchanged; read prints Markdown unchanged; usage and non-2xx responses exit non-zero.
- `bin/briefing` stays unchanged and compatible.

## Approach (Checklist)
- [x] **Step 0: Recon** — Inspect memo entity/repository/service, security chains, briefing CLI, tests, issue, and repository instructions.
- [x] **Step 1: Public list tracer (RED→GREEN)** — Add one failing anonymous integration test for deterministic list output, then implement minimal dedicated response records, repository/service query, and `GET /api/public/memos` endpoint. Add authenticated and anonymous cases proving private/deleted rows never appear, so authentication cannot bypass the repository's `findPublic*` boundary.
- [x] **Step 2: Incremental list boundaries (RED→GREEN)** — Add focused failing tests for strict-exclusive `updatedAfter`, same-timestamp ID ordering, zero-based pagination, defaults, and invalid page/limit/timestamp; implement only required query/validation behavior.
- [x] **Step 3: Public read tracer (RED→GREEN)** — Add failing authenticated and anonymous integration tests for byte-exact UTF-8 Markdown and `text/markdown; charset=UTF-8`; prove private, deleted, and missing IDs all return identical 404 status before implementing `/api/public/memos/{id}/content` solely through `MemoService.getPublicMemo`.
- [x] **Step 4: CLI tracer (RED→GREEN)** — Add process-level JUnit tests backed by JDK `HttpServer`, then add `bin/website` with strict grammar, curl URL encoding, byte-preserving stdout, and HTTP failure propagation.
- [x] **Step 5: Documentation** — Document exact endpoint/CLI contracts, timestamp semantics, pagination, examples, and unchanged briefing CLI.
- [x] **Step 6: Rollout / Rollback** — No schema/config/security-chain changes; deploy application normally. Roll back by reverting the feature commit.

## Validation
- **Commands to run:**
  - Focused Gradle tests after each RED/GREEN slice.
  - `./gradlew test`
  - CLI process tests against a local HTTP fixture.
  - `git diff --check` and full diff review.
  - Independent plan review and independent pre-commit code review.
- **Expected output:** All focused/full tests pass; public list/read work anonymously; private/missing reads return 404; list ordering/filtering are deterministic; CLI preserves JSON/Markdown and exits non-zero on HTTP/usage failures.

## Risks & Rollback
- **Risks:** Timestamp timezone ambiguity from existing `LocalDateTime`; accidental private-data exposure; unstable pagination if ordering is incomplete; shell URL encoding mistakes.
- **Mitigations:** Document server-local ISO format, reuse public repository methods, add `id` tie-breaker, use curl query encoding, cover security and Unicode boundaries with tests.
- **Rollback steps:** Revert the feature commit; no database migration or persistent-state rollback required.

## Open Questions

- None. Issue scope and existing architecture provide sufficient defaults.
