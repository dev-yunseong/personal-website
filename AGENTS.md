# Project Agent Instructions

## Scope and Precedence

This file is the repository-level entrypoint for coding agents.

Read `.agents/docs/project.md` before non-trivial work. Repository-specific
commands, constraints, and narrower instructions take precedence over general
workflow defaults.

## Core Philosophy

Prefer maintainable architecture, explicit structure, scalable design,
deterministic behavior, and modular systems.

Avoid quick hacks, giant rewrites, overengineering, hidden side effects, and
meaningless abstractions.

## Project Workflow

Before non-trivial implementation, follow:

- `.agents/docs/workflow.md`
- `.agents/docs/testing.md`
- `.agents/skills/writing-plan/SKILL.md`

For tracked Git work, follow:

- `.agents/docs/issue.md`
- `.agents/docs/branch.md`
- `.agents/docs/commit.md`
- `.agents/docs/pull-request.md`

Keep changes incremental. Explain architecture decisions, tradeoffs, and risk
points before editing. Use project-local skills when installed and applicable.

Canonical user-level preferences:

- `~/.agents/docs/coding-style.md`
- `~/.agents/docs/performance.md`

When GitHub CLI authentication appears invalid inside the sandbox but the user
says their session is valid, request escalated execution and retry with the
user's session credentials before asking them to re-authenticate.

## Communication

Use the caveman skill. For technical responses, explain why first, then code.
Include relevant tradeoffs and preserve existing architecture. Avoid large code
dumps, unrelated refactors, unnecessary renaming, and pattern-first design.

## Repository Guidelines

### Project Structure and Modules

This is a Java 21 Spring Boot personal website. Main code lives under
`src/main/java/dev/yunseong/website`, organized by feature packages such as
`blog`, `ai`, `manage`, and `global`. Thymeleaf templates are in
`src/main/resources/templates`, with reusable fragments in
`templates/fragments`. Static assets live in `src/main/resources/static`
(`css`, `js`, verification files, and favicon). SQL initialization is in
`src/main/resources/sql`. Tests mirror production packages under
`src/test/java/dev/yunseong/website`.

Production is deployed at `https://yunseong.dev`; use it as the visual
reference when changing UI.

### Build, Test, and Development Commands

- `./gradlew bootRun`: run the app locally on port `8080`.
- `./gradlew test`: run the JUnit 5 test suite.
- `./gradlew build`: compile, test, and produce the Spring Boot artifact.
- `./gradlew clean build`: rebuild from a clean Gradle output directory.
- `docker build --secret id=GITHUB_USERNAME --secret id=GITHUB_TOKEN -t personal-website .`:
  build the production image with private Maven package credentials.

Use the Gradle Wrapper rather than a system Gradle install.

### Coding Style and Naming

Use standard Java style with 4-space indentation. Keep packages feature-oriented
and match existing suffixes: `*Controller`, `*Service`, `*Repository`, and
`*Config`. Domain models should not use framework suffixes. Keep Thymeleaf
templates lowercase and path-based, for example `memo/view.html` or
`console/dashboard.html`. Prefer constructor injection. Use Lombok only where it
fits surrounding code.

### Testing

Tests use Spring Boot Test, JUnit Platform, Spring Security Test, and H2 for the
test runtime database. Name tests with the `*Test` suffix and place them in the
matching package under `src/test/java`. Add controller tests for route and
security behavior, and service tests for business logic. Run `./gradlew test`
before opening a PR.

### Git and Pull Requests

Recent history uses short imperative commits and occasional Conventional Commit
prefixes, especially `feat:` and `fix:`. Prefer messages such as
`fix: isolate chat state per conversation`.

Branch names must follow `<issue-label>/<issue-num>` using the exact GitHub
label, for example `enhancement/123`. Pull requests should include a concise
summary, linked issue when applicable, test results, and screenshots for
user-facing template or CSS changes.

### Security and Configuration

Local configuration imports optional `.env` properties. Never commit
`APPLICATION_USER`, `APPLICATION_PW`, `GITHUB_TOKEN`, database credentials, S3
keys, OpenAI-related keys, or other secrets. Private GitHub Maven package access
depends on `GITHUB_USERNAME` and `GITHUB_TOKEN`.

## Final Principle

Optimize for long-term maintainability, developer clarity, scalable
architecture, and real-world operation—not temporary hacks or visually
impressive complexity.
