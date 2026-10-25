# 2026-10-25 — 로그인과 console 설명 문구 제거

- Date: 2026-10-25
- GitHub Issue: #221, draft pull request #222 후속 변경
- Status: 구현 및 검증 완료

## Goal

로그인은 제목과 입력·선택·로그인 버튼만 있는 한 열 폼으로 줄인다. Console의
장식 문구, 반복 안내, 연결 방법 설명을 제거한다.

## Non-goals

인증 동작, 전송 명령, 보고서 내용, 통계 기능, 공개 사이트 디자인 변경.

## Context / Constraints

오류, CSRF, 접근성 label, 로그인 유지 기간, 보고 상태와 갱신 시각은 유지한다.

## Approach (Checklist)
- [x] **Step 0: Recon** — 로그인/console template, CSS와 기존 테스트 참조 확인.
- [x] **Step 1: Implementation** — HTML 2개 track과 테스트 track 분리. 부모가 CSS에서 삭제된 요소의 규칙을 제거하고 한 열 폼으로 변경.
- [x] **Step 2: Tests** — 기존 인증·보고 테스트, 실제 desktop와 390px 표시, screen capture 갱신.
- [x] **Step 3: Rollout / Rollback** — draft pull request #222에 추가. 배포하지 않으며 commit revert로 복구.

## Validation
- **Commands to run:** `./gradlew test` 707개 통과, `git diff --check` 통과, 실제 로그인·빈 목록·보고서 표시 확인. 실제 HTTP 응답의 390px 로그인과 프로젝트 표시 확인. 네 장의 screen capture 갱신. 실제 휴대폰은 사용하지 않음.
- **Expected output:** 문구와 장식만 제거되며 로그인·로그인 유지·보고·관리자 메뉴가 동작한다.

## Risks & Rollback
- **Risks:** 로그인 안내는 줄였지만 유지 기간과 오류를 남긴다. Agent 연결 설명은 README와 설치 skill에서 제공한다.
- **Rollback steps:** 후속 commit을 revert한다.

## Open Questions

없음. 실제 agent 보고 내용과 갱신 지연 안내는 제거하지 않는다.
