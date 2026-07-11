# Development Workflow

## Why

Small, explicit steps reduce regressions and make review, rollback, and handoff
predictable.

## Work Classification

Trivial work includes documentation typos, isolated formatting fixes, and
deterministic one-line configuration changes.

Non-trivial work includes behavior changes, investigated bug fixes, dependency
or schema changes, cross-module refactors, and user-facing workflow changes.
Create a concise plan with `.agents/skills/writing-plan/SKILL.md` before
non-trivial implementation.

## End-to-End Flow

1. Confirm goal, scope, acceptance criteria, constraints, and non-goals.
2. Read `.agents/docs/project.md`, relevant code, tests, configuration, and recent changes.
3. Link or create an issue for non-trivial tracked development.
4. Create a branch using the exact issue label and issue number.
5. Write a concise implementation plan.
6. Identify architecture impact, tradeoffs, risks, and rollback.
7. Implement the smallest coherent change.
8. Follow `testing.md`.
9. Review the complete diff for scope, correctness, and accidental churn.
10. Commit coherent units.
11. Open a PR with validation evidence and remaining risks.
12. Address review without hiding unresolved concerns.

## Change Rules

- Preserve existing architecture unless the task requires changing it.
- Keep unrelated cleanup out of scope.
- Add abstractions only when they remove demonstrated complexity or match an established pattern.
- Keep migrations backward-compatible when practical.
- Prefer reversible rollout for high-risk behavior.

## Stop Conditions

Pause when requirements conflict, destructive action lacks approval, required
credentials are unavailable, validation reveals a blocking pre-existing
failure, or scope expands beyond the agreed issue or plan.
