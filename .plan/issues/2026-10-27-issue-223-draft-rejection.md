# 2026-10-27 — 초안 기각 및 삭제 확인 추가

- Date: 2026-10-27
- GitHub Issue: #223
- Status: 구현 및 검증 완료

## Goal
초안 검토 화면에서 확인을 거쳐 초안을 기각하고 삭제할 수 있게 합니다.

## Non-goals
원문 삭제, 기각 이력 저장, 화면 구조 변경은 제외합니다.

## Context / Constraints
기존 관리자 세션, CSRF, revision 검증을 사용합니다. 최신 main에서 enhancement/223을 분리하고 기존 작업 폴더는 보존했습니다.

## Approach (Checklist)
- [x] **Step 0: Recon**: 승인 서비스와 검토 화면을 확인했습니다.
- [x] **Step 1: Implementation**: 삭제 API, 서비스, 확인창과 버튼을 추가했습니다.
- [x] **Step 2: Tests**: Java 전체 테스트, Node 테스트, 실제 브라우저 확인 취소와 삭제 및 목록 이동을 검증했습니다.
- [x] **Step 3: Rollout / Rollback**: 별도 스키마 변경은 없습니다. 운영 배포는 수행하지 않았습니다.

## Validation
- **Commands to run:** WSL Java 21에서 `./gradlew test`, `node --test src/test/js/draft-review.test.cjs`, `git diff --check`.
- **Expected output:** 테스트 통과 및 공백 오류 없음.
- **Results:** Java 712개, JavaScript 4개 통과. 첫 Java 실행에서는 Windows 체크아웃의 CRLF로 기존 CLI 테스트 11개가 실패했습니다. `bin/briefing`을 Git 원본과 같은 LF로 복원한 뒤 전체 재실행이 통과했습니다. CLI 내용 변경은 없습니다.
- **Browser:** H2 테스트 앱에서 삭제 확인 취소 시 초안 유지, 확인 시 삭제와 목록 이동, 삭제 후 404를 확인했습니다. `docs/screenshots/draft-rejection.png`와 `docs/screenshots/draft-rejection-mobile.png`를 캡처했습니다.

## Risks & Rollback
- **Risks:** 삭제한 초안을 복원하는 기능은 없습니다. 확인창과 revision 검사로 오삭제를 방지합니다.
- **Rollback steps:** 이번 기능 커밋을 되돌립니다. 이미 삭제된 데이터는 복원하지 않습니다.

## 문구 수정 검증
버튼을 `초안 삭제`, 확인창을 “이 초안을 삭제할까요? 삭제한 초안은 복구할 수 없습니다.”로 변경했습니다. 관련 템플릿 테스트와 JavaScript 테스트 4개가 통과했습니다.

중복된 `다시 검토` 링크를 제거하고 충돌 시 새로고침 안내를 표시합니다.

## Open Questions
없음.
