# 2026-10-29 — 큐레이터 GPT-6 Luna 적용 및 API 오류 진단 로그 추가

- Date: 2026-10-29
- GitHub Issue: #229
- Status: Ready for review

## Goal
기본 채팅 모델을 gpt-6-luna로 바꾸고 API 오류의 진단 정보를 서버 로그에 남긴다.

## Non-goals
Responses API 전환, DB/RAG/MCP 구조 변경과 배포는 포함하지 않는다.

## Context / Constraints
Spring AI 1.1.2와 기존 Chat Completions 구조를 유지한다. GPT-6 Luna의 Chat Completions 도구 호출에는 reasoning_effort=none이 필요하다. 전체 응답 본문과 인증 헤더는 로그에 남기지 않는다.

## Approach (Checklist)
- [x] **Step 0: Recon**: 공식 모델 문서와 현재 오류 처리, 테스트를 확인했다.
- [x] **Step 1: Implementation**: 기본 모델과 도구 호출 옵션을 변경하고 중첩 HTTP 예외의 오류 필드를 기록한다.
- [x] **Step 2: Tests**: 오류 로그 회귀 테스트와 실제 API 스트리밍 및 도구 호출을 확인했다. 최신 main 기준 전체 테스트 739개가 모두 통과했다.
- [x] **Step 3: Rollout / Rollback**: improve/229 브랜치로 검토를 준비했다. 배포는 별도이며 커밋을 되돌려 롤백한다.

## Validation
- **Commands to run:** `./gradlew test`, `git diff --check`.
- **Expected output:** 전체 테스트가 통과하고 이번 변경 외의 파일이 PR에 포함되지 않는다.
- **Live API:** gpt-6-luna, temperature=1.0, reasoning_effort=none으로 일반 스트리밍, add_numbers(19, 23) 호출, 결과 42를 반영한 최종 응답이 모두 HTTP 200이었다.
- **Spring AI:** Java 21과 Spring AI 1.1.2에서 application-ai.yml을 읽은 실제 ChatClient가 LUNA_STREAM_OK를 반환했다. @Tool 계산 함수가 정확히 한 번 실행되고 최종 응답 TOOL_RESULT=42를 반환했다. 외부 호출용 임시 테스트는 일반 테스트 소스에 포함하지 않았다.
- **Limits:** DB, RAG, MCP를 포함한 전체 애플리케이션 흐름과 배포는 검증 범위에 포함하지 않았다.

- **Full suite result:** 최신 main 기반 소스와 이번 변경을 WSL Linux 임시 경로에 옮겨 Java 21로 전체 테스트 739개를 실행했고 BUILD SUCCESSFUL이었다. Windows Git의 core.autocrlf=true가 bin/briefing을 CRLF로 내보내 처음에는 기존 CLI 테스트 11개가 실패했다. Git 원본은 LF임을 확인하고 테스트 복사본만 LF로 복원한 뒤 재실행했다. bin/briefing의 Git 내용 변경은 없다.

## Risks & Rollback
- **Risks:** reasoning-effort none 적용으로 추론 동작이 달라진다. API가 오류 message에 요청 일부를 포함할 수 있어 로그 접근 관리가 필요하다. 각 필드는 2,000자로 제한하고 줄바꿈을 이스케이프한다.
- **Rollback steps:** 이번 커밋을 되돌려 이전 기본 모델과 오류 로그 동작을 복원한다. OPENAI_CHAT_MODEL 환경변수는 기본 모델보다 우선한다.

## Open Questions
- 없음.
