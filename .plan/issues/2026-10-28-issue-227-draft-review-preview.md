# 2026-10-28 — 초안 검토 Markdown 미리보기 추가

- Date: 2026-10-28
- GitHub Issue: #227
- Status: Implemented; awaiting review and deployment

## Goal

초안 검토 화면에서 변경 내용과 렌더링된 미리보기를 전환하고, 승인 전에 표·코드·이미지·링크를 확인할 수 있게 한다.

## Non-goals

편집기, 자동 공개, Mermaid·KaTeX 등 스크립트 실행은 추가하지 않는다.

## Context / Constraints

기존 작업은 별도 worktree에 보존한다. Memo.getHtml()과 공개 본문 CSS를 재사용하고, 미리보기는 sandbox srcdoc 프레임 및 CSP로 관리자 DOM에서 격리한다. 승인·삭제의 인증, CSRF와 revision 검증은 유지한다.

## Approach (Checklist)
- [x] **Step 0: Recon** 초안 검토 템플릿, 본문 렌더러와 기존 테스트 확인
- [x] **Step 1: Implementation** Bootstrap 탭, 격리된 미리보기 문서와 반응형 프레임 추가
- [x] **Step 2: Tests** 표·링크·코드 렌더링, HTML 격리 및 승인·삭제 회귀 검증; 데스크톱·모바일 화면 확인
- [ ] **Step 3: Rollout / Rollback** PR 검토와 배포 후 초안 화면 확인; 문제 시 커밋 되돌림

## Validation
- **Commands to run:** `./gradlew test`, `git diff --check`, 브라우저 탭 전환 및 작은 화면 확인
- **Expected output:** Markdown 렌더링 정상, 스크립트와 프레임 탈출 차단, 기존 승인·삭제 동작 유지
- **Results:** Java 21의 전체 테스트 715개 통과. 로컬 H2 서버의 headless Chromium에서 탭·키보드 전환, 표·코드·이미지, 스크립트 격리 및 390px 화면의 가로 넘침 부재를 확인했다. `cua_repl` 초기화 실패로 개인 브라우저 대신 별도 headless 테스트를 사용했다. 스크린샷은 `docs/screenshots/draft-preview-desktop.png`, `docs/screenshots/draft-preview-mobile.png`에 저장했다.

## Risks & Rollback
- **Risks:** 초안의 raw HTML 실행, 작은 화면의 가로 넘침, 외부 이미지·스타일 로딩 실패
- **Rollback steps:** 미리보기 추가 커밋을 되돌린다. DB 및 저장된 초안은 변경하지 않는다.

## Open Questions
- 없음. 미리보기는 정적 본문을 표시하고 스크립트 기반 콘텐츠는 실행하지 않는다.
