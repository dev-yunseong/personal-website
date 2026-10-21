# Commit Workflow

## Why

Each commit should represent one coherent change and remain safe to review or
revert independently.

## Format

Prefer Conventional Commits where type adds useful context:

```text
<type>(<optional-scope>): <imperative summary>
```

Examples:

```text
feat(ai): add conversation memory
fix(blog): isolate upload directory
docs: document local test setup
```

Short imperative subjects without a prefix remain acceptable when clearer and
consistent with nearby history.

## Rules

- Keep subject at 50 characters or fewer when practical.
- Use imperative present tense and no trailing period.
- Explain non-obvious motivation or tradeoffs in the body.
- Do not mix unrelated behavior, formatting, and refactoring.
- Do not commit secrets, generated noise, or local-only configuration.

## Writing Style

Follow [writing-style.md](writing-style.md) when adding or editing text covered
by this document. Its wording and title rules supersede older tone examples;
preserve project-specific workflows, templates, terminology, and localization.
For UI copy, retain the sentence forms defined in this document.
