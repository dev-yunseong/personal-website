# Project Context

## Overview

- Product: Personal website and publishing/admin application
- Primary users: Public visitors and the site owner/admin
- Core domain: Blog and memo publishing, mini apps, chat/AI features, and site administration
- Runtime environment: Java 21, Spring Boot 3.5, Gradle, server-rendered Thymeleaf

## Architecture

- Entry points: Spring Boot application and MVC controllers under `src/main/java/dev/yunseong/website`
- Main modules: `blog`, `ai`, `manage`, and `global`
- Dependency direction: Feature controllers call feature services and repositories; `global` provides shared configuration and site-level behavior
- External systems: PostgreSQL/pgvector, OpenAI, S3-compatible storage, GitHub Packages, and configured MCP clients
- Persistent data: PostgreSQL through Spring Data JPA/JDBC and S3-compatible object storage

## Commands

| Purpose | Command |
|---|---|
| Install dependencies | Gradle resolves dependencies during build; no separate install command |
| Run locally | `./gradlew bootRun` |
| Format | TODO — no dedicated formatter task verified |
| Lint | TODO — no dedicated lint task verified |
| Type-check | `./gradlew compileJava` |
| Unit tests | `./gradlew test` |
| Integration tests | TODO — no separate integration-test task verified |
| Build | `./gradlew build` |
| Clean build | `./gradlew clean build` |
| Production image | `docker build --secret id=GITHUB_USERNAME --secret id=GITHUB_TOKEN -t personal-website .` |

## Constraints

- Supported platforms: Any environment supporting Java 21; production uses a container image
- Compatibility requirements: Spring Boot 3.5 and Java 21
- Performance constraints: Measure before optimizing; avoid blocking or redundant external calls on request paths
- Security or privacy requirements: Never commit application credentials, database/S3/OpenAI secrets, or GitHub package tokens

## Ownership

- Maintainers: TODO
- Sensitive modules: Security configuration, AI configuration, database configuration, S3 storage, and management routes
- Changes requiring explicit review: Authentication/authorization, schema or persistence changes, secret/config handling, production deployment, and destructive data operations
