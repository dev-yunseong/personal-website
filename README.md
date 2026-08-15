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

Flyway applies the migrations in `src/main/resources/db/migration` on first
start. The app listens on `http://localhost:8080`; sign in with
`APPLICATION_USER` / `APPLICATION_PW`.

## Public memo automation

`bin/website` exposes public memos to scheduled tools without scraping rendered
HTML. It never authenticates and uses only the same public-only repository
queries as the anonymous website.

```sh
export WEBSITE_URL=http://localhost:8080   # defaults to https://yunseong.dev

bin/website memo list
bin/website memo list --updated-after 2026-08-15T10:00:00 --page 0 --limit 100
bin/website memo read 42
```

`memo list` prints the API JSON unchanged:

```json
{
  "items": [
    {"id": 42, "name": "/notes/example", "updatedAt": "2026-08-15T10:30:00"}
  ],
  "page": 0,
  "limit": 100,
  "hasNext": false
}
```

`updated-after` is a strict-exclusive filter (`updatedAt > value`). Its value is
an ISO local date-time without an offset, interpreted in the website server's
local time. Pages are zero-based; `limit` must be between 1 and 100. Results are
ordered by `updatedAt`, then memo ID, both ascending. Offset pagination is stable
for a static result set; a consumer that overlaps with concurrent edits should
start slightly before its last watermark and deduplicate by memo ID.

`memo read` writes the stored Markdown bytes to stdout, including trailing
newlines. Private, deleted, and missing memo IDs all return HTTP 404 and a
non-zero CLI exit status.

The underlying public API is:

```text
GET /api/public/memos?updatedAfter=<yyyy-MM-ddTHH:mm:ss>&page=0&limit=100
GET /api/public/memos/<id>/content
```

The list endpoint returns metadata only. The content endpoint returns
`text/markdown; charset=UTF-8`.

## Agent briefings

An external agent publishes daily briefings; the site stores them and shows a
whole day at `/briefing`. Generating and scheduling them happens outside this
repository — what lives here is the intake API, the page, and the CLI.

A briefing is a memo named `/private/briefing/<kind>/<yyyy-MM-dd>`. The kind is
only a path segment, so a new kind (`news`, `jobs`, anything after that) starts
existing the first time something is published under that name. There is no
registry to update. Publishing the same kind and date again replaces that entry
rather than adding one, which makes a retried cron run harmless.

The memos are private, so briefings stay out of the blog list, the category
tree, search, and the sitemap. The `/briefing` page itself is public.

### Setup

```sh
BRIEFING_TOKEN=<a long random string>
```

The agent sends it as `X-Briefing-Token`. It is deliberately not the admin
account: it cannot reach `/admin/**`, and rotating it is one variable. Leave it
unset and the intake API answers 401 to everything — it never falls open.

That fail-closed default has one confusing consequence worth knowing before you
hit it: a server with no token configured and a client sending the wrong token
produce the identical `401 Missing or invalid X-Briefing-Token`, because saying
which one it is would tell an unauthenticated caller whether the server has a
secret at all. If you are getting a 401 you did not expect, check the server's
startup log — it warns when `BRIEFING_TOKEN` is unset — and confirm the client
is pointed at the right host with `BRIEFING_URL`.

### Publishing

`bin/briefing` is a thin wrapper over `curl`; the API is plain text in and out,
so a markdown body needs no escaping. The title is the body's first `#` heading.

```sh
export BRIEFING_TOKEN=...
export BRIEFING_URL=http://localhost:8080   # defaults to https://yunseong.dev

printf '# 오늘의 뉴스\n\n본문\n' | bin/briefing publish news
bin/briefing publish jobs 2026-08-12 < body.md   # explicit date, default is today
bin/briefing last news                           # most recent briefing of a kind
bin/briefing kinds                               # kinds published recently
```

Or without the script:

```sh
curl -X POST "$BRIEFING_URL/api/agent/briefings/news" \
  -H "X-Briefing-Token: $BRIEFING_TOKEN" \
  -H 'Content-Type: text/plain; charset=utf-8' \
  --data-binary @body.md
```

### Tests

```sh
./gradlew test
```

Tests run against in-memory H2 and need no infrastructure, but they still
need the GitHub Packages credentials from step 1 to resolve the build
classpath.
