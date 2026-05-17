# Repository Guidelines

## Project Structure & Module Organization

This is a Java 21 Spring Boot personal website. Main code lives under `src/main/java/dev/yunseong/website`, organized by feature packages such as `blog`, `ai`, `manage`, and `global`. Thymeleaf templates are in `src/main/resources/templates`, with reusable fragments in `templates/fragments`. Static assets live in `src/main/resources/static` (`css`, `js`, verification files, and favicon). SQL initialization is in `src/main/resources/sql`. Tests mirror production packages under `src/test/java/dev/yunseong/website`.

Production is deployed at `https://yunseong.dev`; use it as the visual reference when changing UI.

## Build, Test, and Development Commands

- `./gradlew bootRun`: run the app locally on port `8080`.
- `./gradlew test`: run the JUnit 5 test suite.
- `./gradlew build`: compile, test, and produce the Spring Boot artifact.
- `./gradlew clean build`: rebuild from a clean Gradle output directory.
- `docker build --secret id=GITHUB_USERNAME --secret id=GITHUB_TOKEN -t personal-website .`: build the production image with private Maven package credentials.

The build uses Gradle Wrapper, so prefer `./gradlew` over a system Gradle install.

## Coding Style & Naming Conventions

Use standard Java style with 4-space indentation. Keep packages feature-oriented and match existing suffixes: `*Controller`, `*Service`, `*Repository`, `*Config`, and domain model names without framework suffixes. Keep Thymeleaf templates lowercase and path-based, for example `memo/view.html` or `console/dashboard.html`. Prefer constructor injection when adding Spring components. Use Lombok only where it already fits the surrounding code.

## Testing Guidelines

Tests use Spring Boot Test, JUnit Platform, Spring Security Test, and H2 for test runtime database support. Name tests after the class or behavior under test, using `*Test` suffix, and place them in the matching package under `src/test/java`. Add controller tests for route/security behavior and service tests for business logic. Run `./gradlew test` before opening a PR.

## Commit & Pull Request Guidelines

Recent history uses short imperative commits and occasional Conventional Commit prefixes, especially `feat:` and `fix:`. Prefer messages like `fix: isolate chat state per conversation` or `feat: add mini-app admin API`. Branch names must follow `<issue-label>/<issue-num>` using the exact GitHub label, for example `enhancement/123`. Pull requests should include a concise summary, linked issue when applicable, test results, and screenshots for user-facing template or CSS changes.

## Security & Configuration Tips

Local configuration imports optional `.env` properties. Do not commit secrets such as `APPLICATION_USER`, `APPLICATION_PW`, `GITHUB_TOKEN`, database credentials, S3 keys, or OpenAI-related keys. Private GitHub Maven package access depends on `GITHUB_USERNAME` and `GITHUB_TOKEN`.
