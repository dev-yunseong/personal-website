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

The curator talks to any OpenAI-compatible endpoint. `OPENAI_BASE_URL` and
`OPENAI_CHAT_MODEL` move the chat model off OpenAI — for DeepSeek:

```sh
OPENAI_BASE_URL=https://api.deepseek.com
OPENAI_CHAT_MODEL=deepseek-chat
OPENAI_API_KEY=<your DeepSeek key>
```

Embeddings are a separate connection, because DeepSeek serves no
`/v1/embeddings`. Set `EMBEDDING_BASE_URL` and `EMBEDDING_API_KEY` to keep RAG
on OpenAI while the chat runs on DeepSeek. Leave them unset and embeddings
follow the chat provider: retrieval then fails on every request, the curator
logs a `WARN` and answers without the blog context instead of erroring out, and
the daily RAG sync logs its failure and retries the next day.

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

## CLI memo drafts

### Navigation and draft reads

```sh
bin/website memo tree                 # full public tree
bin/website memo tree notes           # recursive subtree
bin/website memo list --category notes --limit 20  # direct children only

export DRAFT_TOKEN=...
bin/website memo tree notes           # includes attached drafts
bin/website memo read 42 --draft      # draft for published memo 42
bin/website memo read notes/new-post --draft  # draft-only target
```

With `DRAFT_TOKEN`, `tree` and `list` use authenticated draft-aware navigation.
Without it they show only public memos. A token never grants access to other
private memos or permission to publish. Drafts appear at their **target paths**,
not under a `private/draft` directory:

```text
/notes/
├── spring-guide [id=42] [draft]
└── new-post [draft only]
```

Tree order is alphabetical; memo IDs can be passed to `memo read`. A memo can
also contain children, in which case its category line retains its ID/marker.
Unknown or private scopes produce an empty tree. Paths accept an optional leading
and trailing slash. Category lists contain direct children, not descendants or
the category's own memo. Existing `--page`, `--limit`, and `--updated-after`
options work with `--category`.

Draft-aware list JSON adds `hasDraft`; draft-only entries have `id: null` and a
public target `name`. Their `updatedAt` is the latest of draft/published updates.
`memo read <id>` always reads the published body; `--draft` requires a token and
returns 404 if there is no draft. Bad tokens fail rather than silently hiding drafts.

Public tree API: `GET /api/public/memos/tree?path=/notes` (UTF-8 text).
Authenticated routes, all with `X-Draft-Token`:
`GET /api/agent/drafts/tree?path=/notes`,
`GET /api/agent/drafts/memos?category=/notes`, and
`GET /api/agent/drafts/content?memoId=42` (or `?target=/notes/new-post`).

### Submission and approval

Configure a separate `DRAFT_TOKEN` on the server and export the same value on
the CLI host. An unset token disables draft access. This token can submit and
read drafts; publication requires logging into the admin console.

```sh
export DRAFT_TOKEN=...
bin/website memo draft notes/my-post article.md
cat article.md | bin/website memo draft notes/my-post -
```

The response contains the private draft ID, name, and console review URL.
That draft ID is for console review; use the target path or published memo ID
with `memo read ... --draft`.
The target accepts an optional leading slash; omit it when invoking Git Bash
from PowerShell to avoid its native argument path conversion.
Content is stored verbatim at `/private/draft/notes/my-post`; submitting that
target again replaces its pending draft. Targets must be public memo paths.
The limit is 100,000 characters, including whitespace.

Open **Admin Console → 메모 초안**, inspect the line diff, then click
**승인하고 반영**. Approval sends `PUT /admin/console/drafts/{id}/approval`
and creates or replaces `/notes/my-post`. Existing memo IDs and links survive.
Publication and removal of the pending draft happen in one database transaction.
If the target or draft changed since review, approval returns 409; reload the
review before approving again. Approved drafts disappear from the pending list.
For very large documents the diff shows full removal/addition to bound memory.

The form-encoded intake endpoint is `POST /api/agent/drafts` with `target` and
`content` fields and an `X-Draft-Token` header. `WEBSITE_URL` selects the server
and defaults to `https://yunseong.dev`, as with the read commands.

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
bin/briefing show news 2026-08-12                # briefing for an exact date
bin/briefing kinds                               # kinds published recently
```

Or without the script:

```sh
curl -X POST "$BRIEFING_URL/api/agent/briefings/news" \
  -H "X-Briefing-Token: $BRIEFING_TOKEN" \
  -H 'Content-Type: text/plain; charset=utf-8' \
  --data-binary @body.md

curl "$BRIEFING_URL/api/agent/briefings/news/2026-08-12" \
  -H "X-Briefing-Token: $BRIEFING_TOKEN"
```

### Tests

```sh
./gradlew test
```

Tests run against in-memory H2 and need no infrastructure, but they still
need the GitHub Packages credentials from step 1 to resolve the build
classpath.
