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

`OPENAI_API_KEY` must be set to something, but it does not have to be a real
key. Spring AI validates it while building the beans, not when calling the
API, so an empty value fails startup at `openAiApi` — and with
`spring.main.lazy-initialization=true` it just fails one step later at
`openAiEmbeddingModel` instead. Any non-empty placeholder starts the app with
every page working; only the curator itself fails, and the chat UI renders
that as an error. Put a real key in when you need the curator.

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
