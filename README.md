# personal-website

Personal website and publishing/admin application. Java 21, Spring Boot 3.5,
server-rendered Thymeleaf, PostgreSQL with pgvector, S3-compatible storage.

## Local development

### 1. Credentials

The build pulls one dependency from a private GitHub Packages repository, so
Gradle needs both a username and a token with `read:packages`:

```sh
export GITHUB_USERNAME=<your-github-username>
export GITHUB_TOKEN=<token with read:packages>
```

Setting only `GITHUB_TOKEN` fails with `Username must not be null!` — the
username is read separately and has no default.

### 2. Application config

```sh
cp .env.example .env
```

`OPENAI_API_KEY` is optional. Leave it empty and the app detects that at
startup, logs a `WARN`, and boots with the curator switched off:

- `/public/chat` renders a notice instead of the composer
- `GET /api/public/chat` and `POST /api/admin/miniapps/generate` answer `503`
- RAG sync and mini app metadata generation stay idle

Every other page behaves exactly as it does with a key. Put a real key in when
you want the curator, and the whole graph comes back with no other change.

Detection only looks at whether the key is blank, so a typo that resolves to an
empty value silently disables the curator — the startup `WARN` is the only
signal. To pin the behaviour explicitly, set `app.curator.enabled` yourself; an
explicit value always wins over detection.

`TAVILY_API_KEY` and the GeoLite2 database are optional — a missing GeoIP file
only leaves the geo fields null.

### 3. Infrastructure

```sh
docker compose up -d
```

This starts PostgreSQL with pgvector and MinIO, and creates the bucket with
anonymous read access so uploaded images resolve through `S3_PUBLIC_URL`.
Both read their credentials from the same `.env`, so they cannot drift apart
from `DATABASE_URL` and the `S3_*` settings.

### 4. Run

```sh
./gradlew bootRun
```

Flyway applies `V1__initial_schema.sql` and `V2__seed_profile_memo.sql` on
first start. The app listens on `http://localhost:8080`; sign in with
`APPLICATION_USER` / `APPLICATION_PW`.

### Tests

```sh
./gradlew test
```

Tests run against in-memory H2 and need no infrastructure, but they still
need the GitHub Packages credentials from step 1 to resolve the build
classpath.
