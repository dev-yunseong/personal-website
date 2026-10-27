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

## GeoLite2 databases

Request statistics read two local MaxMind databases: GeoLite2 City for country
and city, and GeoLite2 ASN for the datacenter bot signal. The image does not
contain them. It is published publicly to `ghcr.io`, and the GeoLite2 EULA
forbids redistributing the databases and requires deleting a copy within 30
days of an update, which an immutable image tag cannot do.

Download both files with your own MaxMind account (or keep them current with
MaxMind's `geoipupdate`) and mount the directory into the container:

```sh
docker run -v /maxmind:/app/geoip:ro ... ghcr.io/dev-yunseong/personal-website
```

The default paths are `/app/geoip/GeoLite2-City.mmdb` and
`/app/geoip/GeoLite2-ASN.mmdb`. Set `GEOIP_DATABASE_PATH` and
`GEOIP_ASN_DATABASE_PATH` to read them from elsewhere. A missing file is not an
error: geo fields stay null, the datacenter signal stays off, and the startup
log says which path it looked at.

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
the CLI host. An unset token disables draft access. This token can submit,
read and rename drafts and upload images; publication requires logging into
the admin console.

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

### CLI 초안 제목 변경과 이미지 업로드

```sh
bin/website memo draft-rename notes/old framework/spring/new
bin/website memo draft-upload ./image.png
```

두 명령 모두 `DRAFT_TOKEN`이 필요합니다. `draft-rename`은 초안의 제목 경로만
변경하고 ID와 본문(본문의 `#` 제목 포함)을 유지합니다. 경로는 공개 대상 경로로
지정하며 `/private/draft`를 붙이지 않습니다. 공개 글은 변경하지 않습니다.
대상 경로에 공개 글 또는 다른 초안이 있으면 HTTP 409, 원본 초안이 없으면
404를 반환합니다. 같은 경로로 변경하면 기존 초안을 그대로 반환합니다.
변경 전 검토 화면의 승인은 충돌로 거부되므로 다시 검토해야 합니다.

`draft-upload`는 PNG, JPEG, GIF, WebP 확장자의 파일을 multipart로 전송하고
`url`과 `markdown`이 담긴 JSON을 출력합니다. 서버에서 실제 이미지 읽기 가능 여부를
검증하며, 지원 여부는 서버의 이미지 디코더에도 따릅니다. 기존 관리자 업로드와
동일한 S3 저장소와 리사이즈 정책(최대 변 길이 512px)을 사용합니다.
이미지 URL은 공개 URL이며 초안 승인 여부와 별개입니다. 이 명령은 초안 본문을
자동으로 수정하지 않습니다. 반환된 URL을 본문에 넣고 `memo draft`로 제출하세요.
같은 파일을 다시 업로드하면 새 객체가 생성됩니다.

API는 `POST /api/agent/drafts/rename` (`source`, `target` 폼 필드)과
`POST /api/agent/drafts/images` (`file` multipart 필드)이며 `X-Draft-Token`을
요구합니다. 이미지 외 파일과 빈 파일은 거부합니다. 토큰에는 공개 승인이나
일반 비공개 문서 수정 권한이 없습니다. 새 CLI 명령은 대응하는 서버 버전 배포 후
사용할 수 있습니다.

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

### 관리자 로그인 유지와 프로젝트 console

`/admin`과 직접 로그인한 사용자는 `/admin/console`로 이동합니다. 비공개
페이지에서 로그인 요청을 받았다면 원래 페이지로 돌아갑니다. `/login`에서
로그인 유지를 선택하면 기존 browser session과 Spring Security의 database
기반 remember-me token으로 30일 동안 로그인할 수 있습니다. 별도의 JWT
access/refresh protocol은 추가하지 않았습니다.

로그아웃하면 다른 기기의 remember-me token을 포함해 해당 사용자의 저장된
로그인 token을 폐기합니다. 다른 기기의 기존 session까지 종료하지는 않습니다.
Cookie에는 HttpOnly와 SameSite=Lax를 적용하고 HTTPS 요청에는 Secure도
적용합니다. 운영 환경에서는 HTTPS forwarding 설정을 유지하세요.
`APPLICATION_PW`를 변경할 때에는 `DELETE FROM persistent_logins`도 실행해
기존 로그인 유지 token을 폐기해야 합니다. 비밀번호 변경만으로는 폐기되지 않습니다.
새 application을 실행하기 전에 Flyway migration V6를 적용하세요.

관리자 메뉴에서 프로젝트, 사이트 운영, 대화 기록, 콘텐츠 관리로 이동할 수
있습니다. 기존 통계는 `/admin/console/traffic`으로 이동했으며 `tab` parameter가
있는 이전 console 주소도 통계를 표시합니다. 프로젝트 보고서는 관리자만
볼 수 있습니다. 등록된 프로젝트만 표시하며 repository를 자동 탐색하지 않습니다.

Agent는 기존 command-line interface로 최신 보고서와 위젯 구성을 전송합니다.

```sh
# BRIEFING_TOKEN은 서버 설정과 같아야 합니다. 개인 환경변수로 관리하세요.
WEBSITE_URL=https://yunseong.dev bin/website project report personal-website docs/examples/project-report.json
# 표준 입력도 지원합니다.
cat docs/examples/project-report.json | bin/website project report personal-website -
```

HTTP endpoint는 `PUT /api/agent/projects/{project-id}`이며 `Content-Type:
application/json`과 `X-Briefing-Token` header가 필요합니다. 저장하면 204와
`Location` header로 비공개 프로젝트 주소를 반환합니다. 기존 `BRIEFING_TOKEN`은
briefing과 프로젝트 보고서 작성 권한을 모두 가집니다. 관리자 페이지 로그인
권한은 없습니다. Agent host가 침해되면 해당 token을 교체하세요.

[보고서 예시](docs/examples/project-report.json)의 형식을 사용하세요.

- 프로젝트 식별자: 소문자, 숫자, 하이픈으로 1~64자. 첫 글자는 소문자 또는 숫자.
- 필수 필드: `name` 100자, `agent` 100자, `summary` 2000자, `status`,
  `staleAfterMinutes` 5~10080, `widgets` 0~12개. 배열 순서대로 위젯을 표시합니다.
- `status`: `RUNNING`, `BLOCKED`, `COMPLETED`, `IDLE`.
- `METRIC`: `title`, `value`. `TABLE`: `title`, `columns`, `rows`.
  `LINE`: `title`, `points`. 각 point에는 `label`과 숫자 `value`가 필요합니다.
- 제목, metric 값, 열 이름, point label은 각각 100자 이하입니다. 표는 1~8열,
  최대 100행이며 각 셀은 비어 있지 않은 500자 이하 문자열입니다. 그래프는
  1~100개 point이며 유한한 숫자와 최대 절댓값 1e12를 허용합니다.
- 보고서 전체 크기는 64 KiB 이하입니다.

같은 식별자로 전송하면 이전 보고서를 교체하므로 프로젝트별 보고 담당 agent를
하나로 정하세요. 서버 수신 시각을 한국 표준시로 표시하며 `staleAfterMinutes`가
지나면 agent가 보고한 상태와 별도로 갱신 지연을 표시합니다. 그래프 이력은
agent가 보고서에 포함합니다. 보고서 archive, 임의 HTML/JavaScript 실행,
agent polling, 지시 전달 기능은 이번 범위에 포함하지 않습니다.

`bin/website skills install`은 `website-project-report` skill을 현재 작업
디렉터리의 `.agents/skills`에 설치합니다. 다른 위치를 지정하려면 경로를
인자로 전달하세요. 예를 들어 `bin/website skills install .claude/skills`는
`.claude/skills/website-project-report`에 설치합니다. 설치한 뒤에는 해당
skill의 `SKILL.md` 지침에 따라 `scripts/website`를 호출할 수 있습니다.
다른 프로젝트에서는 설치된 `scripts/website`의 절대 경로로 실행하세요.
기존 설치 경로가 있으면 다시 설치하지 않으며, 기존 파일을 덮어쓰지 않습니다.
설치 명령은 network 요청을 하지 않고 global 설정도 변경하지 않습니다.
설치 단계에는 인증 token이 필요하지 않으며 `.env`나 인증 정보도 복사하지 않습니다.

다른 프로젝트에 처음 설치할 때에는 그 프로젝트에서 원본 명령의 절대 경로로
실행하세요.

```sh
cd /path/to/your-project
/path/to/personal-website/bin/website skills install
```

설치한 뒤 agent에게 `website-project-report` skill을 사용해 프로젝트 상태를
보고하도록 요청할 수 있습니다. 전송할 때에는 `BRIEFING_TOKEN`이 필요합니다.
