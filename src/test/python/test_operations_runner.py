import importlib.util
import json
import os
import stat
import sys
import tempfile
import unittest
from pathlib import Path


RUNNER_PATH = Path(__file__).parents[3] / "bin" / "operations-runner.py"
SPECIFICATION = importlib.util.spec_from_file_location("operations_runner", RUNNER_PATH)
operations_runner = importlib.util.module_from_spec(SPECIFICATION)
assert SPECIFICATION.loader is not None
sys.modules[SPECIFICATION.name] = operations_runner
SPECIFICATION.loader.exec_module(operations_runner)


class FakeRunnerClient:
    def __init__(self, claim=None):
        self.claim = claim
        self.reports = []
        self.results = []

    def publish_report(self, configuration, report):
        self.reports.append(report)

    def claim_action(self, configuration):
        return self.claim

    def publish_action_result(self, configuration, request_id, result):
        self.results.append((request_id, result))


class OperationsRunnerTest(unittest.TestCase):
    def write_script(self, directory, name, body):
        path = directory / name
        path.write_text("#!/bin/sh\nset -eu\n" + body, encoding="utf-8")
        path.chmod(path.stat().st_mode | stat.S_IXUSR)
        return path

    def write_config(self, directory, script_root, extra=""):
        config = directory / "dashboard.yml"
        config.write_text(
            "\n".join([
                "version: 1",
                f"scriptRoot: {script_root}",
                "pollSeconds: 5",
                "reportIntervalSeconds: 60",
                "project:",
                "  slug: home-operations",
                "  name: Home operations",
                "  agent: operations-runner",
                "  summary: Host status.",
                "  staleAfterMinutes: 10",
                "sections:",
                "  - id: games",
                "    title: Games",
                "    links: []",
                "    widgets:",
                "      - id: palworld-status",
                "        title: Palworld",
                "        type: METRIC",
                "        check:",
                "          script: status.sh",
                "          intervalSeconds: 5",
                "          timeoutSeconds: 1",
                "    actions:",
                "      - palworld-start",
                "actions:",
                "  - key: palworld-start",
                "    label: Start Palworld",
                "    description: Start the game server.",
                "    requiresConfirmation: true",
                "    script:",
                "      script: start.sh",
                "      timeoutSeconds: 1",
                extra,
            ]),
            encoding="utf-8",
        )
        return config

    def configuration(self, extra=""):
        temporary_directory = tempfile.TemporaryDirectory()
        self.addCleanup(temporary_directory.cleanup)
        directory = Path(temporary_directory.name)
        scripts = directory / "scripts"
        scripts.mkdir()
        self.write_script(scripts, "status.sh", "printf 'Running\\n'\n")
        self.write_script(scripts, "start.sh", "printf 'Started\\n'\n")
        return self.write_config(directory, scripts, extra), scripts

    def test_compiles_yaml_to_sections_and_global_actions(self):
        config_path, _ = self.configuration()
        configuration = operations_runner.load_configuration(config_path)
        runner = operations_runner.OperationsRunner(configuration, FakeRunnerClient())

        runner.refresh_checks(force=True)
        payload = operations_runner.build_report(configuration, runner.state)

        self.assertEqual([], payload["widgets"])
        self.assertEqual("Running", payload["sections"][0]["widgets"][0]["value"])
        self.assertEqual(["palworld-start"], [action["key"] for action in payload["actions"]])
        self.assertEqual(["palworld-start"], payload["sections"][0]["actionKeys"])

    def test_rejects_unknown_yaml_field(self):
        config_path, _ = self.configuration("unknown: value")

        with self.assertRaisesRegex(operations_runner.ConfigurationError, "unknown fields"):
            operations_runner.load_configuration(config_path)

    def test_rejects_script_outside_root_and_symlink_escape(self):
        config_path, scripts = self.configuration()
        outside = scripts.parent / "outside.sh"
        self.write_script(scripts.parent, "outside.sh", "printf outside\\n\n")
        (scripts / "status.sh").unlink()
        os.symlink(outside, scripts / "status.sh")

        with self.assertRaisesRegex(operations_runner.ConfigurationError, "inside scriptRoot"):
            operations_runner.load_configuration(config_path)

    def test_timeout_and_output_limit_stop_processes(self):
        with tempfile.TemporaryDirectory() as temporary_directory:
            scripts = Path(temporary_directory)
            timeout_script = self.write_script(scripts, "timeout.sh", "sleep 2\n")
            exact_script = self.write_script(scripts, "exact.sh", "yes x | head -c 16384\n")
            output_script = self.write_script(scripts, "output.sh", "yes x | head -c 20000\n")

            timeout = operations_runner.execute_script(operations_runner.ScriptDefinition(timeout_script, 1))
            exact = operations_runner.execute_script(operations_runner.ScriptDefinition(exact_script, 5))
            output = operations_runner.execute_script(operations_runner.ScriptDefinition(output_script, 5))

        self.assertEqual("FAILURE", timeout.state)
        self.assertIn("timeout exceeded", timeout.output)
        self.assertEqual("SUCCESS", exact.state)
        self.assertEqual(operations_runner.MAX_OUTPUT_BYTES, len(exact.output.encode()))
        self.assertEqual("FAILURE", output.state)
        self.assertIn("output limit exceeded", output.output)
        self.assertLessEqual(len(output.output.encode()), operations_runner.MAX_OUTPUT_BYTES + 32)

    def test_unknown_claimed_action_reports_failure_without_running_script(self):
        config_path, _ = self.configuration()
        configuration = operations_runner.load_configuration(config_path)
        client = FakeRunnerClient({"id": "request-3", "projectSlug": "home-operations", "actionKey": "unknown"})

        operations_runner.OperationsRunner(configuration, client).execute_claimed_action(client.claim)

        self.assertEqual("request-3", client.results[0][0])
        self.assertEqual("FAILURE", client.results[0][1].state)

    def test_failed_action_and_once_payload_are_reported(self):
        config_path, scripts = self.configuration()
        self.write_script(scripts, "start.sh", "exit 7\n")
        configuration = operations_runner.load_configuration(config_path)
        client = FakeRunnerClient({"id": "request-8", "projectSlug": "home-operations", "actionKey": "palworld-start"})

        runner = operations_runner.OperationsRunner(configuration, client)
        runner.run_once()
        self.assertEqual([], client.results)
        runner.execute_claimed_action(client.claim)

        self.assertEqual(1, len(client.reports))
        self.assertEqual("FAILURE", client.results[0][1].state)
        self.assertEqual("", client.results[0][1].output.strip())

    def test_table_output_is_bounded_json(self):
        config_path, _ = self.configuration()
        configuration = operations_runner.load_configuration(config_path)
        widget = operations_runner.WidgetDefinition("table", "Table", "TABLE", configuration.sections[0].widgets[0].check)
        structured = operations_runner.structured_widget(
            widget, operations_runner.CommandResult("SUCCESS", json.dumps({"columns": ["name"], "rows": [["palworld"]]}))
        )
        self.assertEqual(["name"], structured["columns"])
        self.assertEqual([["palworld"]], structured["rows"])


if __name__ == "__main__":
    unittest.main()
