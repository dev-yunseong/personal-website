---
name: writing-plan
description: Generate a work plan markdown file under .plan/general/ or .plan/issues/ with strict naming conventions. Use when asked to write a plan or when starting any non-trivial change.
---

# writing-plan Skill

## Trigger Conditions

Use when the user asks for a plan, before a non-trivial change without a plan
file, or when preparing a PR-reviewable plan.

## Inputs

- `title` (required): Short human-readable title
- `github_issue` (optional): Issue number, `#number`, or issue URL

## Output Rules

- Use current date as `YYYY-MM-DD` in filename and body.
- Translate non-English title to a short English slug.
- Lowercase; replace spaces/underscores with `-`; remove special characters;
  collapse repeated separators; limit slug to 50 characters.

Without issue, create:

```text
.plan/general/<YYYY-MM-DD>-<slug>.md
```

With issue, normalize and validate digits, then create:

```text
.plan/issues/<YYYY-MM-DD>-issue-<number>-<slug>.md
```

If target exists, append `-2`, `-3`, and so on. GitHub issue verification with
`gh issue view` is optional unless explicitly requested.

## Content

Load `assets/PLAN_TEMPLATE.md`, replace all placeholders, and keep every
section. Fill known context; leave unresolved items in Open Questions.

## Return

Report clickable file path, 3–5 goal bullets, and open questions.
