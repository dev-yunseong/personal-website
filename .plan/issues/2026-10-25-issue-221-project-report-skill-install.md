# 2026-10-25 — 프로젝트 보고 skill 설치 명령 추가

- Date: 2026-10-25
- GitHub Issue: #221, pull request #222 후속 변경
- Status: 구현 및 검증 완료

## Goal

`bin/website skills install [skills-directory]`로 프로젝트 보고 skill을 설치한다.
기본 경로는 현재 작업 디렉터리의 `.agents/skills`이고 그 안에
`website-project-report` 디렉터리를 생성한다. 설치본만으로 보고서 작성과
전송이 가능하도록 skill 문서, 기존 명령 스크립트, 보고서 예시를 함께 설치한다.

## Non-goals

원격 skill 다운로드, package manager 추가, 전역 자동 설치, 기존 skill 강제 덮어쓰기,
agent별 설정 변경, polling, 지시 전달.

## Context / Constraints

기존 POSIX shell 명령 구조와 전송 기능을 재사용한다. 기존 사용자 파일과
설치된 skill을 보존한다. Token이나 `.env`는 복사하지 않는다. 설치 작업에는
네트워크와 인증 정보가 필요하지 않다.

## Approach (Checklist)
- [x] **Step 0: Recon** — 기존 명령, 보고서 형식, 테스트, skill 작성 규칙 확인.
- [x] **Step 1: Implementation** — 부모: bin/website. 문서 subagent: skills/website-project-report/SKILL.md, README.md. 테스트 subagent: WebsiteSkillInstallTest.java. 파일 범위 분리.
- [x] **Step 2: Tests** — 임시 프로젝트에 기본/사용자 경로 설치, 공백 포함 경로, 기존 파일·symlink 보존, 설치본 단독 보고 전송, 잘못된 인자, 기존 명령 회귀 검증.
- [x] **Step 3: Rollout / Rollback** — 기존 draft pull request에 추가한다. 운영 배포는 하지 않는다. 설치한 프로젝트 skill을 삭제하면 설치를 취소할 수 있다. 실제 프로젝트나 global 경로 설치는 수행하지 않고 임시 디렉터리만 사용했다.

## Validation
- **Commands to run:** `sh -n bin/website`, skill validator, 집중 테스트, `./gradlew test`, `git diff --check`. 전체 707개 테스트 통과. source와 임시 설치본 모두 skill validator 통과.
- **Expected output:** 네트워크 요청 없이 완전한 skill bundle을 설치하고 기존 대상은 변경하지 않는다. 설치본 스크립트는 보고서를 기존 endpoint로 전송한다.

## Risks & Rollback
- **Risks:** 설치 대상은 사용자가 지정한 filesystem 경로이므로 기존 대상은 생성 단계에서 거부하고 실패 시 이번에 만든 대상만 정리한다. Repository 밖에서 실행해도 스크립트 위치로 설치 source를 찾는다.
- **Rollback steps:** 후속 commit을 revert한다. 설치본은 해당 프로젝트의 website-project-report 디렉터리만 삭제한다.

## Open Questions

없음. Agent별 global 경로는 자동 변경하지 않고 설치 경로 인자로 지원한다.
