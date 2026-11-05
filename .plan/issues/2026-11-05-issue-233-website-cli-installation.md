# 2026-11-05 — website command line interface 설치와 관리자 설치 명령 복사 추가

- Date: 2026-11-05
- GitHub Issue: #233
- Status: Implemented; draft pull request #234

## Goal

관리자 페이지에서 `curl -fsSL https://yunseong.dev/cli/install.sh | sh`를 복사하고 사용자 컴퓨터에 website를 설치한다.

## Non-goals

package registry 배포, production 서버 변경, 토큰 배포, 기존 명령 변경.

## Context / Constraints

`bin/website`를 단일 원본으로 유지한다. 설치 script와 실행 파일은 `/cli/install.sh`, `/cli/website`로 익명 접근을 허용한다. 기본 설치 위치는 `~/.local/bin/website`이며 관리자 권한과 shell 설정 파일 변경 없이 설치한다. 같은 명령으로 업데이트한다. 최신 `main`의 `website skills install`을 지원하기 위해 skill과 예제도 `.website`에 설치한다. 설치 안내는 `/admin/console/install`의 별도 페이지에서 제공한다. 왼쪽 관리자 메뉴 하단에 설치 링크를 표시하며 모바일에서는 마지막 메뉴로 표시한다. 대시보드에는 설치 안내를 넣지 않는다.

## Approach (Checklist)
- [x] **Step 0: Recon** — 관리자 template, 공개 경로 whitelist, Gradle resource 처리, 기존 shell process 테스트 확인.
- [x] **Step 1: Implementation** — 두 worker가 각각 설치 script·Gradle packaging·접근 정책과 관리자 안내·복사 버튼·README를 담당했다. 부모 agent가 최신 관리자 대시보드와 통합하고 Docker build 제외 규칙을 수정했다.
- [x] **Step 2: Tests** — 설치·업데이트·다운로드 실패 시 기존 파일 보존, 공개 resource와 관리자 접근 제한, 관리자 layout 렌더링, Chromium 복사 성공·실패 및 모바일 너비 검증 완료. 최신 main 통합 후 `./gradlew build`에서 772개 테스트 통과. 뒤에 추가한 command 교체 실패 복구 테스트까지 포함하여 `CliInstallerTest` 12개도 별도로 통과했다.
- [x] **Step 3: Rollout / Rollback** — Docker build의 제외 규칙과 원본 파일 복사를 반영했다. 생성된 배포 artifact에서 공개 파일 4개가 원본과 동일함을 확인했다. production 배포 후 관리자 안내와 공개 다운로드를 확인해야 한다. 변경을 revert하면 제거할 수 있다.

## Validation
- **Commands to run:** `sh -n src/main/resources/static/cli/install.sh`, focused Gradle tests, `./gradlew test`, `git diff --check`.
- **Expected output:** 인증 없이 shell script 다운로드, 실행 가능한 website 설치, 실패 시 기존 파일 보존, 관리자 페이지 접근 제한 유지.
- **Results:** 전체 build 및 772개 테스트 통과. `sh -n`, `node --check`, `git diff --check` 통과. Chromium에서 1280px 및 375px 표시, 자동 복사와 권한 거부·기능 미지원 시 수동 선택 확인. screen capture는 `/tmp/website-cli-desktop.png`, `/tmp/website-cli-mobile.png`에 저장했다. 브라우저 검증은 실제 template fragment와 stylesheet로 구성한 로컬 fixture를 사용했다. production 서버와 Docker image 자체는 실행하지 않았다.

## Risks & Rollback
- **Risks:** 다운로드 실패와 PATH 누락. 다운로드와 문법 검증을 마친 뒤 파일을 교체하고 PATH 안내를 제공한다. build 과정에서 실행 파일을 복사하여 원본 불일치를 방지한다.
- **Rollback steps:** 이 변경을 revert하고 기존 서버 image로 재배포한다. 사용자가 설치한 파일은 직접 삭제한다.

## Open Questions
- 없음. 설치 주소는 사용자 지정 `/cli/install.sh`를 사용한다.

## 설치 페이지 이동

- [x] 대시보드의 설치 안내를 `/admin/console/install`로 이동하고 관리자 메뉴 하단에 설치 링크 추가.
- [x] 로그인 필요, 메뉴 선택 표시, 대시보드에서 설치 안내 제거 검증. `./gradlew test`: 776개 통과, 실패·오류·건너뜀 0개.
- [x] 데스크톱·모바일 screen capture 갱신. 기존 draft pull request #234에 반영한다.
