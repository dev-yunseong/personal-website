---
name: website-project-report
description: Prepare and publish a truthful status report for a project using the personal website project report command. Use for project status reporting, not for building or modifying a website or creating skills.
---

# 프로젝트 상태 보고서

프로젝트의 현재 상태를 JSON 보고서로 작성하고 사용자가 요청한 경우 personal website에 전송합니다. 이 skill은 website 또는 skill 제작 작업에는 적용하지 않습니다.

1. 현재 프로젝트의 실제 상태와 확인 가능한 근거를 확인합니다. 확인하지 않은 진행률, 테스트 결과, 완료 상태를 만들지 않습니다.
2. 이 skill의 `assets/project-report.json`을 현재 작업 디렉터리의 파일로 복사한 뒤 실제 정보에 맞게 수정합니다. 예를 들어 `project-report.json`을 작업 파일로 사용할 수 있습니다. 예시 내용을 현재 사실인 것처럼 그대로 보고하지 않습니다.
3. 프로젝트 식별자는 소문자·숫자·하이픈으로 1~64자이며 첫 글자는 소문자 또는 숫자여야 합니다. 첫 전송으로 프로젝트가 등록됩니다. `name`, `agent`, `status`, `summary`, `staleAfterMinutes`, `widgets` 필드를 채웁니다. `name`과 `agent`는 비어 있지 않은 100자 이하 문자열이며 `summary`는 비어 있지 않은 2000자 이하 문자열입니다. `status`는 `RUNNING`, `BLOCKED`, `COMPLETED`, `IDLE` 중 하나이며 `staleAfterMinutes`는 5~10080입니다. `widgets`는 최대 12개이며 배열 순서대로 표시됩니다.
4. 위젯은 `METRIC`의 `title`과 `value`, `TABLE`의 `title`, `columns`, `rows`, `LINE`의 `title`과 `points` 형식을 사용합니다. 위젯 제목, metric 값, 열 이름은 각각 비어 있지 않은 100자 이하 문자열입니다. 표는 1~8열, 최대 100행이며 각 셀은 비어 있지 않은 500자 이하 문자열이어야 합니다. 그래프는 1~100개 점이며 각 점은 `label`과 유한한 숫자 `value`를 가집니다. label은 비어 있지 않은 100자 이하 문자열이고 숫자의 절댓값은 1e12 이하여야 합니다. 보고서 크기는 64 KiB 이하여야 합니다.
5. 사용자가 보고서 제출을 요청한 경우에만 전송합니다. `BRIEFING_TOKEN`은 필수이며 개인 환경변수로 설정되어 있어야 합니다. `WEBSITE_URL`은 선택 사항이며 기본값은 `https://yunseong.dev`입니다. token을 파일이나 보고서에 넣지 않습니다. 전송 명령은 다음과 같습니다.

   ```sh
   sh "<skill-path>/scripts/website" project report <project-id> "<report.json>"
   ```

   `<skill-path>`은 설치된 `website-project-report` 폴더의 경로입니다. personal-website checkout 밖에서도 실행할 수 있으며, 다른 프로젝트에서 사용하려면 설치된 skill의 절대 경로를 지정합니다.

같은 project ID로 전송하면 기존 보고서를 교체합니다. 서버 수신 시각이 보고서 시각이며, `staleAfterMinutes` 이후에는 갱신 지연 상태로 표시됩니다. 보고서의 private page URL은 `WEBSITE_URL/admin/console/projects/<project-id>`입니다. 명령이 오류를 반환하거나 성공 여부를 확인할 수 없으면 보고서가 전송되었다고 말하지 말고 오류를 보고합니다. 설치 자체는 파일을 복사하며 network 요청을 하지 않습니다.
