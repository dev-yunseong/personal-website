# Pull Request Workflow

## Why

PR should expose intent, verification evidence, and risk without requiring the
reviewer to reconstruct development history.

## Before Opening

- Confirm issue acceptance criteria.
- Update plan to reflect final implementation.
- Review full diff against the default branch.
- Remove debug code and unrelated churn.
- Run required validation.
- Document migration, configuration, and rollback needs.

## Body Template

```markdown
## Why

## What Changed

## Validation
- [ ] Command or manual check

## Risks

## Rollback

Closes #123
```

Include screenshots for user-facing template or CSS changes.

## Merge Criteria

Acceptance criteria satisfied, required checks pass, approvals complete,
unresolved risks explicitly accepted, and deployment/migration order documented.
