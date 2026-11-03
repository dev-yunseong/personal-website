# 2026-11-01 — YAML operations dashboard와 host runner 추가

- Date: 2026-11-01
- GitHub Issue: #231
- Status: Ready for review

## Goal

기존 Project Console에 agent report와 host operations을 같이 표시한다. Host의 YAML이 section, widget, link, action을 정의하고, 별도 runner가 status script와 등록된 action script를 실행한다. 관리자는 Project Console에서 상태를 확인하고 action을 요청할 수 있다.

## Non-goals

Browser에서 임의 command, script 경로, argument, environment variable를 입력하지 않는다. Web application container에 Docker socket이나 host deploy directory를 mount하지 않는다. 실시간 terminal, destructive cleanup, YAML editor, 여러 host 관리, 실패한 action 자동 재실행은 첫 version에 포함하지 않는다.

## Context / Constraints

기존 `/api/agent/projects/{slug}`는 project report JSON을 검증해 최신 snapshot 하나를 저장한다. Browser 관리 경로는 session과 Cross-Site Request Forgery token으로 보호된다. Operations runner는 별도 `RUNNER_TOKEN`과 `X-Runner-Token`을 사용하고, project report agent와 action 실행 권한을 분리한다. 현재 사용자가 수정한 `.env.example`, `README.md`, `src/main/resources/application-ai.yml`은 이 issue의 commit에 포함하지 않는다.

## Approach (Checklist)
- [x] **Step 0: Recon** — Project Console, agent security chain, migration, command line interface, production Docker Compose 배치, host Python과 PyYAML을 확인했다.
- [x] **Step 1: Implementation** — `ProjectReportContent`에 backward-compatible section, link, action catalog를 추가한다. V7 migration과 action queue service, 관리자 request endpoint, runner claim/result endpoint, runner 전용 security chain을 추가한다. `bin/operations-runner.py`가 YAML을 검증하고 report publish, action claim, allowlisted script 실행, result publish를 수행하게 한다. Project 상세 page에 section, link, action 상태와 최근 결과를 표시한다.
- [x] **Step 2: Tests** — Report backward compatibility과 validation, action state transition과 lease, authentication·Cross-Site Request Forgery, output limit, escaped output을 Java test로 검증한다. Python unit test로 YAML validation, script root containment, symbolic link, timeout, output limit, unknown action을 검증한다. 전체 Gradle test와 desktop·mobile Browser flow를 확인한다.
- [x] **Step 3: Rollout / Rollback 준비** — Production에서는 `RUNNER_TOKEN`을 추가하고 V7 migration을 먼저 적용한다. 준비한 systemd service example로 runner를 시작하고 `/home/yunseong/deploy/server-dashboard/dashboard.yml`과 script directory를 배치한다. Rollback은 runner를 중지하고 application commit을 revert하며, 추가 table은 남겨도 기존 기능에 영향이 없다.

## Validation
- **Commands to run:** `./gradlew test`, `python3 -m unittest discover -s src/test/python -p 'test_*.py'`, `sh -n bin/website`, `git diff --check`
- **Expected output:** 기존 project report client가 변경 없이 작동한다. 관리자만 action을 queue에 추가한다. Runner token은 admin page에 접근하지 못하고, project report token은 action을 claim하지 못한다. Runner는 YAML에 등록된 script만 실행한다.

## Risks & Rollback
- **Risks:** Docker group의 권한은 host root에 준하므로 runner account와 YAML·script 파일 쓰기 권한을 분리해야 한다. 동시 claim이 중복되면 같은 action이 두 번 실행될 수 있으므로 database lock과 lease를 적용한다. Action output에 secret을 출력하지 않도록 script contract을 문서화하고 16 KiB로 제한한다.
- **Rollback steps:** Runner systemd service를 중지하고 issue commit을 revert한다. V7 table은 application이 참조하지 않으면 남겨도 된다.

## Open Questions
- Production rollout에서 dedicated runner account를 새로 만들지, 현재 `yunseong` account의 user systemd service로 시작할지는 구현 검증 후 결정한다.
