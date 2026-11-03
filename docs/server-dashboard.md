# Host operations dashboard 설정

`bin/operations-runner.py`는 host에서 실행합니다. Website container에는 Docker socket, script directory, deployment directory를 mount하지 않습니다.

1. Host에 Python 3와 PyYAML을 준비합니다. Ubuntu에서는 `sudo apt install python3 python3-yaml`로 설치할 수 있습니다.
2. `/home/yunseong/deploy/server-dashboard/scripts`를 만들고 executable script를 둡니다.
3. [server-dashboard.yml](examples/server-dashboard.yml)을 `/home/yunseong/deploy/server-dashboard/dashboard.yml`로 복사합니다. `scriptRoot`와 Palworld placeholder를 실제 host path로 바꿉니다.
4. `/home/yunseong/deploy/server-dashboard/runner.env`를 mode `600`으로 만들고 `RUNNER_TOKEN=...`을 저장합니다. application environment와 별도로 `RUNNER_TOKEN`을 runner service에만 설정합니다.
5. 다음 command로 config와 API connection을 한 번 확인합니다.

   ```sh
   RUNNER_TOKEN=... python3 /path/to/operations-runner.py --config /home/yunseong/deploy/server-dashboard/dashboard.yml --once
   ```

6. [operations-runner.service](examples/operations-runner.service)를 `~/.config/systemd/user/operations-runner.service`에 복사하고 `systemctl --user daemon-reload && systemctl --user enable --now operations-runner`로 시작합니다. `--once`는 status check와 report publish만 한 번 수행하며 queued action은 claim하지 않습니다.

YAML은 version 1에서 `project`, `sections`, `widgets`, `links`, `actions`를 정의합니다. `scriptRoot` 밖의 script, symbolic link로 root 밖을 가리키는 script, executable regular file이 아닌 script는 runner가 거부합니다. 설정은 64 KiB 이하입니다.

각 check는 `script`, `intervalSeconds`, `timeoutSeconds`를 가집니다. `METRIC` script는 100자 이하 plain text를 출력합니다. `TABLE` script는 `{"columns":["name"],"rows":[["value"]]}` JSON을 출력합니다. `LINE` script는 `{"points":[{"label":"10:00","value":3}]}` JSON을 출력합니다. TABLE은 최대 8 columns와 100 rows, LINE은 최대 100 points입니다.

Action은 `key`, `label`, `description`, `requiresConfirmation`, `script`를 가집니다. Section의 `actions`는 이 key를 표시 순서대로 참조합니다. Browser는 action key만 queue에 넣고, runner는 local YAML에서 key를 다시 찾아 script를 실행합니다. Browser input, API response의 path, argument, environment variable는 script command에 전달하지 않습니다. 각 process는 별도 process group으로 실행되며 timeout과 stdout/stderr 합산 16 KiB limit을 넘으면 종료됩니다.

`RUNNER_TOKEN`과 script output에 secret을 넣지 마세요. Docker group은 host root에 준하는 권한이므로 runner account, YAML, script의 write permission을 website container와 분리해야 합니다.
