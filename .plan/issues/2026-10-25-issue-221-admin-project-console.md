# 2026-10-25 — 관리자 로그인 유지와 프로젝트 console 추가

- Date: 2026-10-25
- GitHub Issue: #221
- Status: 구현 및 검증 완료

## Goal

새 로그인 페이지에서 로그인 유지를 선택할 수 있다. Console을 관리자 기본
페이지로 사용하며 agent가 보고한 프로젝트 상태와 위젯을 확인한다. 관리자
공통 메뉴에서 콘텐츠 관리와 사이트 운영으로 이동한다.

## Non-goals

Agent 지시 전달, polling, 자동 실행, 임의 HTML/JavaScript 위젯, frontend framework 변경.

## Context / Constraints

Spring MVC, Thymeleaf, 기존 form/session 인증과 token 기반 agent endpoint를
유지한다. 공개 페이지와 기존 통계 주소의 호환성을 유지한다. 기존 untracked
사용자 파일과 enhancement/200 branch의 commit을 보존한다.

## Approach (Checklist)
- [x] **Step 0: Recon** — 보안 설정, briefing endpoint, console template, command-line interface, 테스트 확인. origin/main에서 enhancement/221 생성.
- [x] **Step 1: Implementation** — Database 기반 remember-me, 새 로그인 페이지, 공통 관리자 layout, /admin/console/traffic, 프로젝트 보고 저장·검증, 지표·표·선 그래프 위젯, 전송 명령과 문서 추가.
- [x] **Step 2: Tests** — 전체 701개 통과. Cookie 복원·회전·만료·재사용·로그아웃 폐기, 보고서 검증·저장·escaping, 명령 전송, 기존 통계 호환성 검증. Browser 로그인·개요·상세·통계 확인. 실제 응답을 390px iframe srcdoc에서 표시해 모바일 layout 확인.
- [x] **Step 3: Rollout / Rollback** — 추가형 V6 migration을 H2와 임시 PostgreSQL 14에서 검증. 운영 배포는 수행하지 않음.

## Validation
- **Commands to run:** `./gradlew test` → 701 tests, 0 failures, 0 errors, 0 skipped. `sh -n bin/website`, `node --check src/main/resources/static/js/project-console.js`, `git diff --check` 통과.
- **Expected output:** 비로그인 console 접근 시 새 로그인 페이지로 이동한다. 로그인 유지 선택 후 session이 없어져도 복원되며 로그아웃 시 token을 폐기한다. 검증된 프로젝트 보고와 위젯만 관리자에게 표시한다.
- **Migration:** 별도 임시 PostgreSQL에서 V6 적용과 두 테이블 insert/query 확인. 운영 database에 접근하지 않음.
- **Browser evidence:** `docs/screenshots/console-login.png`, `docs/screenshots/console-overview.png`, `docs/screenshots/console-project.png`, `docs/screenshots/console-mobile.png`. 실제 모바일 기기는 사용하지 않음.

## Risks & Rollback
- **Risks:** 기존 BRIEFING_TOKEN은 briefing과 프로젝트 보고 작성 권한을 공유한다. 프로젝트별 최신 보고 하나를 저장하며 chart 이력은 agent가 제공한다. 비밀번호를 변경할 때 `DELETE FROM persistent_logins`로 로그인 유지 token도 폐기해야 한다. 다른 기기의 활성 session은 로그아웃 시 종료되지 않는다.
- **Rollback steps:** 기능 commit을 revert하고 추가된 테이블은 유지할 수 있다. 로그인 유지를 전체 취소하려면 persistent_logins를 비운다. 운영 application 실행 전 Flyway V6 적용이 필요하다.

## Open Questions

없음. 로그인 유지는 기존 session 구조에 맞춰 30일 remember-me로 구현했다.
별도 JWT access/refresh protocol은 추가하지 않았다. Agent는 위젯 종류·내용·순서를
정의하고 관리자 layout과 메뉴는 사이트에서 유지한다.
