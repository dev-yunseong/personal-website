# Branch Workflow

## Why

Branch names expose issue intent and linkage without relying on local context.

## Naming

Use the exact GitHub issue label:

```text
<issue-label>/<issue-number>
```

Examples:

```text
enhancement/123
bug/418
documentation/527
```

Rules:

- Prefix must match the primary issue's exact GitHub label.
- Issue number must contain digits only.
- Identify or create the issue before creating the branch.
- Keep one primary issue per branch.

## Lifecycle

Branch from the repository default branch unless project policy says otherwise.
Sync before final validation when divergence matters. Never force-push a shared
branch without coordination. Delete merged branches when no follow-up depends on
them.
