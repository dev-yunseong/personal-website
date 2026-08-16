# 2026-08-16 — 과거 브리핑 CLI 조회

- Date: 2026-08-16
- GitHub Issue: #175
- Status: Completed; PR #176 open

## Goal

인증된 agent API와 얇은 `briefing` CLI를 통해 특정 종류·날짜의 저장된 브리핑 Markdown을 조회한다.

## Non-goals

- 날짜 목록 조회
- 공개 브리핑 UI 변경
- 새 테이블·엔티티·migration 추가
- 기존 브리핑 본문 형식 변경

## Context / Constraints

- `BriefingService.findByKindAndDate`와 private memo 저장 구조를 재사용한다.
- API와 CLI는 UTF-8 `text/plain` 계약을 유지한다.
- controller는 날짜를 `String` path variable로 받고 `BriefingService.parseRequiredDate`로 엄격히 해석해 기존 plain-text 400 handler를 사용한다.
- `show`는 저장 Markdown의 trailing newline까지 byte-for-byte 보존해야 한다. 현재 command substitution 기반 `request`가 후행 개행을 제거하므로, 검증된 `bin/website`의 임시 출력 파일 패턴으로 공통 request를 바꾼다.
- 기존 `publish`, `last`, `kinds` 명령과 endpoint 계약을 유지하고 회귀 테스트한다.
- strict RED-GREEN-REFACTOR로 controller와 CLI의 observable behavior를 검증한다.

## Approach (Checklist)
- [x] **Step 0: Recon** — 기존 controller/service/CLI/test와 저장 경계를 확인한다.
- [x] **Step 1: API TDD** — 날짜별 GET 성공(UTF-8 plain text와 service 인자), 누락 시 exact `No briefing published for kind '<kind>' on <date>.\n` 404, 잘못된 날짜 시 exact `Date must be yyyy-MM-dd.\n` 400과 `text/plain;charset=UTF-8`, service 미호출 테스트를 순서대로 실패시킨 뒤 `GET /{kind}/{date}`를 controller에 직접 추가한다. 기존 `/{kind}/latest` literal route도 회귀 테스트한다.
- [x] **Step 2: Auth integration TDD** — 날짜별 GET이 유효 token으로만 저장 본문을 반환하고 token 누락 시 401인지 running application 경계에서 검증한다.
- [x] **Step 3: CLI TDD** — 로컬 HTTP server 기반 `BriefingCliTest`에서 `show <kind> <date>`의 GET method, exact path, `X-Briefing-Token` header를 검증한다. UTF-8 응답 끝이 0개·1개·여러 개 newline인 세 fixture의 stdout byte-for-byte 보존을 각각 RED→GREEN으로 검증한다. 404는 stdout 비움+stderr exact body/status, 인자 부족·초과는 usage/exit 2로 검증한다. `last` 성공 경로도 공통 request 회귀 테스트로 보호한다.
- [x] **Step 4: Docs** — README와 CLI usage에 새 명령과 직접 GET 예시를 기록한다.
- [x] **Step 5: Validation** — focused tests, 전체 test, clean build, 로컬 HTTP fixture를 사용한 실제 CLI 조회를 수행한다. 운영 smoke test는 endpoint 배포 후 수행한다.
- [x] **Step 6: Review / PR** — diff·security 검사, 독립 review, commit, push, PR #176 생성, GitGuardian check PASS를 확인했다.

## Validation
- **Commands to run:**
  - `./gradlew test --tests '*AgentBriefingApiControllerTest' --tests '*BriefingCliTest'`
  - `./gradlew test`
  - `./gradlew clean build`
  - `briefing show news 2026-08-15`
- **Expected output:** 날짜별 저장 Markdown이 그대로 출력되고, 누락은 404, 잘못된 날짜는 400, 잘못된 CLI 인자는 exit 2이며 전체 build가 성공한다.

## Risks & Rollback
- **Risks:** `/{kind}/{date}` route가 `/{kind}/latest`와 겹칠 수 있으나 Spring literal route 우선순위와 controller tests로 보호한다. CLI URL 조립 시 날짜가 path segment가 되므로 서버의 엄격한 ISO date parsing에 의존한다.
- **Rollback steps:** PR을 revert하면 endpoint와 CLI 명령만 제거되며 저장 데이터·schema 영향은 없다.

## Open Questions
- 없음.
