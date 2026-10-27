# 2026-10-27 — CLI 초안 제목 변경과 이미지 업로드 추가

- Date: 2026-10-27
- GitHub Issue: #225
- Status: Implemented; awaiting production rollout

## Goal

초안 경로만 변경하는 `memo draft-rename`과 이미지 파일을 website 저장소에 올리는 `memo draft-upload` 명령을 추가한다. 운영 반영 후 이전한 블로그 글의 이미지 주소를 자체 저장소 주소로 교체한다.

## Non-goals

공개 승인, 일반 메모 이동, 본문 제목 변경, 이미지 외 파일 업로드는 추가하지 않는다.

## Context / Constraints

기존 DRAFT_TOKEN 인증과 S3StorageService를 재사용한다. 다른 작업의 미커밋 변경을 보존하기 위해 origin/main 기반 worktree를 사용한다. 업로드 이미지는 관리자 업로드와 같은 공개 저장소에 저장된다.

## Approach (Checklist)
- [x] **Step 0: Recon** 기존 CLI, 초안 서비스, 저장소와 테스트 확인
- [x] **Step 1: Implementation** 초안 전용 rename API/서비스와 이미지 전용 업로드 API, CLI 명령 및 README 추가
- [x] **Step 2: Tests** ID·본문 보존, 경로 충돌·인증·파일 형식·CLI 인코딩과 전체 Gradle 테스트
- [ ] **Step 3: Rollout / Rollback** PR 검토 후 배포, CLI 이미지 업로드와 초안 URL 교체 및 읽기 검증

## Validation
- **Commands to run:** `./gradlew test`, `git diff --check`, CLI 업로드 후 반환 URL 및 초안 읽기 검증
- **Expected output:** 전체 테스트 통과, 초안만 변경, 비공개 문서 접근 거부, 58개 이미지의 자체 저장소 URL 확인
- **Results:** 관련 테스트 51개 통과. Linux 임시 복사본의 Java 21 환경에서 전체 테스트 729개 통과. Windows Git Bash의 한글 경로와 PNG 바이너리 업로드 검증 통과. Windows 체크아웃의 기존 `bin/briefing` CRLF는 Linux 검증 복사본에서만 LF로 정상화했다.

## Risks & Rollback
- **Risks:** 잘못된 경로 이동, 동시 목적지 생성, 이미지 업로드의 공개 URL 및 저장 용량 증가. 기존 업로드 정책에 따라 이미지 긴 변이 512px로 축소될 수 있다.
- **Rollback steps:** DB 고유 제약과 트랜잭션으로 초안 충돌을 방지한다. 코드 변경은 커밋을 되돌리고, 초안은 백업한 본문으로 복구한다. 공개 이미지는 참조 확인 없이 삭제하지 않는다.

## Open Questions
- 운영 반영은 PR 검토와 배포 완료가 필요하다.
