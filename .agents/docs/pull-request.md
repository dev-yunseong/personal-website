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

## Title

Use Conventional Commit format with a Korean summary:

```text
<type>(<optional-scope>): <한글 요약>
```

A title is a label for the change, not a claim about it. Name the thing that
changed and say what happened to it, and give the number when there is one.
Never write a title a reviewer has to interpret: an aphorism, a figure of
speech, or a sentence stating an insight rather than an edit cannot be checked
against the diff. End the title with a noun such as `적용`, `추가`, or
`이동`, not with a sentence ending such as `~한다`.

| 나쁨 | 좋음 |
| --- | --- |
| `fix(qa): 런은 agent 가 물어봐서 나아간다` | `fix(qa): QA run 에 tool 호출 상한과 실행 시간 제한 적용` |
| `refactor: 구조를 데이터로` | `refactor(qa): loop 상한과 vision 여부를 QaArchSpec 필드로 이동` |
| `feat: 지식은 두 번 모델을 거친다` | `feat(knowledge): knowledge 항목별 검색용 질문 생성 및 색인` |

Write the body in Korean as well. Keep the template headings, code identifiers,
API names, CLI commands, and error strings in their original form.

## Writing Style

Follow [writing-style.md](writing-style.md) when adding or editing text covered
by this document. Its wording and title rules supersede older tone examples;
preserve project-specific workflows, templates, terminology, and localization.
For UI copy, retain the sentence forms defined in this document.
