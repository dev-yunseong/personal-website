# 2026-11-11 — 홈 title, canonical, 공유 이미지, JSON-LD 추가

- Date: 2026-11-11
- GitHub Issue: #235
- Status: Implemented

## Goal

- 홈 `<title>` 과 description 에 한글 이름, 영문 이름, 직무를 넣습니다.
- `pageUrl` 을 지정한 페이지가 canonical link 를 출력하게 합니다.
- `pageImage` 가 없는 페이지의 공유 이미지를 favicon 에서 1200×630 이미지로 바꿉니다.
- 홈이 `Profile` 값으로 만든 `Person`, `WebSite` JSON-LD 를 출력하게 합니다.

## Non-goals

- `meta keywords` 정리, 프로젝트와 수상 항목의 내부 link 추가, memo URL 구조 변경
- 프로필 YAML schema 변경
- `chat.html` 이 `layout.html` 과 겹쳐 출력하는 Open Graph tag 정리

## Context / Constraints

- `layout.html` 은 `pageUrl` 이 없으면 `og:url` 을 `app.base-url` 로 출력합니다. canonical 까지 같은 방식으로 내면 `pageUrl` 을 지정하지 않은 페이지가 모두 홈을 대표 URL 로 가리키게 됩니다. 그래서 canonical 은 `pageUrl` 이 있을 때만 출력합니다.
- 프로필은 private profile memo 에서 읽으므로 JSON-LD 를 template 에 고정 문자열로 둘 수 없습니다.
- JSON-LD 는 `<script>` 안에 escape 없이 들어가므로 `<` 를 `<` 로 바꿔 `</script>` 가 본문에 섞여도 script 가 끊기지 않게 합니다.
- `robots.txt`, `sitemap.xml`, `<html lang="ko">` 는 이미 있습니다.

## Approach (Checklist)
- [x] **Step 0: Recon** `layout.html`, `index.html`, `chat.html`, `MainController`, `Profile`, `SitemapService` 확인
- [x] **Step 1: Implementation**
  - `templates/layout.html`: 기본 title 과 description 변경, canonical 추가, 기본 `og:image` 와 `twitter:card` 변경
  - `templates/index.html`: title 과 description 변경, JSON-LD `<script>` 추가
  - `templates/chat.html`: 공유 이미지와 `twitter:card` 변경
  - `global/service/ProfileJsonLdService.java`: `Profile` 을 JSON-LD 문자열로 변환
  - `global/controller/MainController.java`: `profileJsonLd` model attribute 추가
  - `static/images/og-default.png`: 1200×630 기본 공유 이미지 추가
- [x] **Step 2: Tests** `ProfileJsonLdServiceTest`, `MainControllerTest`, `HomeSeoIntegrationTest`
- [x] **Step 3: Rollout / Rollback** 설정과 schema 변경이 없어 배포만 하면 됩니다.

## Validation
- **Commands to run:** `./gradlew test`
- **Expected output:** 전체 test 통과. 배포 뒤 `https://yunseong.dev` 의 page source 에서 canonical, `og:image`, `application/ld+json` 을 확인합니다.

## Risks & Rollback
- **Risks:** 홈 title 의 한글 이름과 직무 표기는 template 에 고정한 값입니다. 프로필 memo 의 `headline` 을 바꿔도 title 은 따라 바뀌지 않습니다.
- **Rollback steps:** `git revert` 로 되돌립니다.

## Open Questions
- 홈 title 의 직무 표기를 `Backend Developer` 로 둘지, 프로필 `headline` 과 같은 `Software Engineer` 로 맞출지 정해야 합니다.
