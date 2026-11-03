#!/usr/bin/env python3
"""Run the server-side operations dashboard without exposing host access to the website."""

from __future__ import annotations

import argparse
import json
import logging
import os
import select
import signal
import stat
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import yaml


MAX_CONFIG_BYTES = 65_536
MAX_OUTPUT_BYTES = 16 * 1024
MAX_SCRIPT_SECONDS = 300
MAX_WIDGETS = 12
MAX_TABLE_ROWS = 100
MAX_LINE_POINTS = 100
SAFE_ENVIRONMENT = {
    "HOME": "/nonexistent",
    "LANG": "C.UTF-8",
    "LC_ALL": "C.UTF-8",
    "PATH": "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
}


class ConfigurationError(ValueError):
    """The server-owned dashboard configuration is invalid."""


@dataclass(frozen=True)
class ScriptDefinition:
    path: Path
    timeout_seconds: int
    interval_seconds: int | None = None


@dataclass(frozen=True)
class WidgetDefinition:
    identifier: str
    title: str
    widget_type: str
    check: ScriptDefinition


@dataclass(frozen=True)
class ActionDefinition:
    key: str
    title: str
    description: str
    confirmation: bool
    script: ScriptDefinition


@dataclass(frozen=True)
class SectionDefinition:
    identifier: str
    title: str
    links: list[dict[str, str]]
    widgets: list[WidgetDefinition]
    action_keys: list[str]


@dataclass(frozen=True)
class RunnerConfiguration:
    project_slug: str
    project_name: str
    project_agent: str
    project_summary: str
    stale_after_minutes: int
    script_root: Path
    poll_seconds: int
    report_interval_seconds: int
    sections: list[SectionDefinition]
    actions: dict[str, ActionDefinition]


@dataclass
class CommandResult:
    state: str
    output: str


@dataclass
class RunnerState:
    widgets: dict[str, dict[str, Any]] = field(default_factory=dict)
    next_check_at: dict[str, float] = field(default_factory=dict)
    last_report_at: float = 0.0


def require_mapping(value: Any, field_name: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise ConfigurationError(f"{field_name} must be a mapping")
    return value


def require_list(value: Any, field_name: str) -> list[Any]:
    if not isinstance(value, list):
        raise ConfigurationError(f"{field_name} must be a list")
    return value


def require_keys(value: dict[str, Any], allowed: set[str], field_name: str) -> None:
    unknown = set(value) - allowed
    if unknown:
        raise ConfigurationError(f"{field_name} has unknown fields: {', '.join(sorted(unknown))}")


def require_text(value: Any, field_name: str, maximum: int = 100) -> str:
    if not isinstance(value, str) or not value.strip() or len(value) > maximum:
        raise ConfigurationError(f"{field_name} must be a non-blank string up to {maximum} characters")
    return value


def require_identifier(value: Any, field_name: str) -> str:
    identifier = require_text(value, field_name, 64)
    if not identifier[0].isalnum() or any(character not in "abcdefghijklmnopqrstuvwxyz0123456789-" for character in identifier):
        raise ConfigurationError(f"{field_name} must use lowercase letters, digits, and hyphens")
    return identifier


def require_integer(value: Any, field_name: str, minimum: int, maximum: int) -> int:
    if isinstance(value, bool) or not isinstance(value, int) or not minimum <= value <= maximum:
        raise ConfigurationError(f"{field_name} must be an integer from {minimum} to {maximum}")
    return value


def truncate_utf8(value: str, maximum: int) -> str:
    encoded = value.encode("utf-8")
    if len(encoded) <= maximum:
        return value
    return encoded[:maximum].decode("utf-8", errors="ignore")


def validate_url(value: Any, field_name: str) -> str:
    url = require_text(value, field_name, 2_000)
    if any(character.isspace() or ord(character) < 32 for character in url):
        raise ConfigurationError(f"{field_name} cannot contain whitespace or control characters")
    parsed = urllib.parse.urlparse(url)
    if url.startswith("/") and not url.startswith("//"):
        return url
    if parsed.scheme in {"http", "https"} and parsed.netloc:
        return url
    raise ConfigurationError(f"{field_name} must be an absolute HTTP(S) URL or a site-relative path")


def resolve_script(script_root: Path, configured_path: Any, field_name: str) -> Path:
    path = require_text(configured_path, field_name, 500)
    candidate = Path(path)
    if not candidate.is_absolute():
        candidate = script_root / candidate
    try:
        resolved = candidate.resolve(strict=True)
    except OSError as exception:
        raise ConfigurationError(f"{field_name} does not exist: {path}") from exception
    try:
        resolved.relative_to(script_root)
    except ValueError as exception:
        raise ConfigurationError(f"{field_name} must resolve inside scriptRoot") from exception
    mode = resolved.stat().st_mode
    if not stat.S_ISREG(mode) or not mode & (stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH):
        raise ConfigurationError(f"{field_name} must be an executable regular file")
    return resolved


def parse_script(value: Any, script_root: Path, field_name: str, check: bool = False) -> ScriptDefinition:
    mapping = require_mapping(value, field_name)
    allowed = {"script", "timeoutSeconds"}
    if check:
        allowed.add("intervalSeconds")
    require_keys(mapping, allowed, field_name)
    return ScriptDefinition(
        path=resolve_script(script_root, mapping.get("script"), f"{field_name}.script"),
        timeout_seconds=require_integer(mapping.get("timeoutSeconds", 30), f"{field_name}.timeoutSeconds", 1, MAX_SCRIPT_SECONDS),
        interval_seconds=require_integer(mapping.get("intervalSeconds", 60), f"{field_name}.intervalSeconds", 5, 3_600) if check else None,
    )


def load_configuration(config_path: Path) -> RunnerConfiguration:
    try:
        with config_path.open("rb") as config_file:
            contents = config_file.read(MAX_CONFIG_BYTES + 1)
    except OSError as exception:
        raise ConfigurationError(f"Cannot read configuration: {config_path}") from exception
    if len(contents) > MAX_CONFIG_BYTES:
        raise ConfigurationError(f"Configuration must be at most {MAX_CONFIG_BYTES} bytes")
    try:
        document = yaml.safe_load(contents)
    except yaml.YAMLError as exception:
        raise ConfigurationError("Configuration is not valid YAML") from exception
    root = require_mapping(document, "document")
    require_keys(root, {"version", "scriptRoot", "pollSeconds", "reportIntervalSeconds", "project", "sections", "actions"}, "document")
    if root.get("version") != 1:
        raise ConfigurationError("version must be 1")
    try:
        script_root = Path(require_text(root.get("scriptRoot"), "scriptRoot", 2_000)).resolve(strict=True)
    except OSError as exception:
        raise ConfigurationError("scriptRoot does not exist") from exception
    if not script_root.is_dir():
        raise ConfigurationError("scriptRoot must be a directory")
    project = require_mapping(root.get("project"), "project")
    require_keys(project, {"slug", "name", "agent", "summary", "staleAfterMinutes"}, "project")
    sections: list[SectionDefinition] = []
    section_identifiers: set[str] = set()
    widget_identifiers: set[str] = set()
    for section_index, raw_section in enumerate(require_list(root.get("sections"), "sections")):
        field_name = f"sections[{section_index}]"
        section = require_mapping(raw_section, field_name)
        require_keys(section, {"id", "title", "links", "widgets", "actions"}, field_name)
        section_identifier = require_identifier(section.get("id"), f"{field_name}.id")
        if section_identifier in section_identifiers:
            raise ConfigurationError(f"Duplicate section id: {section_identifier}")
        section_identifiers.add(section_identifier)
        links: list[dict[str, str]] = []
        for link_index, raw_link in enumerate(require_list(section.get("links", []), f"{field_name}.links")):
            link = require_mapping(raw_link, f"{field_name}.links[{link_index}]")
            require_keys(link, {"label", "url"}, f"{field_name}.links[{link_index}]")
            links.append({"label": require_text(link.get("label"), "link.label"), "url": validate_url(link.get("url"), "link.url")})
        widgets: list[WidgetDefinition] = []
        for widget_index, raw_widget in enumerate(require_list(section.get("widgets"), f"{field_name}.widgets")):
            widget_field = f"{field_name}.widgets[{widget_index}]"
            widget = require_mapping(raw_widget, widget_field)
            require_keys(widget, {"id", "title", "type", "check"}, widget_field)
            identifier = require_identifier(widget.get("id"), f"{widget_field}.id")
            if identifier in widget_identifiers:
                raise ConfigurationError(f"Duplicate widget id: {identifier}")
            widget_identifiers.add(identifier)
            widget_type = require_text(widget.get("type"), f"{widget_field}.type")
            if widget_type not in {"METRIC", "TABLE", "LINE"}:
                raise ConfigurationError(f"{widget_field}.type must be METRIC, TABLE, or LINE")
            widgets.append(WidgetDefinition(identifier, require_text(widget.get("title"), f"{widget_field}.title"), widget_type,
                                            parse_script(widget.get("check"), script_root, f"{widget_field}.check", check=True)))
        action_keys = [require_identifier(key, f"{field_name}.actions")
                       for key in require_list(section.get("actions", []), f"{field_name}.actions")]
        if len(action_keys) != len(set(action_keys)):
            raise ConfigurationError(f"{field_name}.actions contains duplicate keys")
        sections.append(SectionDefinition(section_identifier, require_text(section.get("title"), f"{field_name}.title"),
                                          links, widgets, action_keys))
    if sum(len(section.widgets) for section in sections) > MAX_WIDGETS:
        raise ConfigurationError(f"At most {MAX_WIDGETS} widgets are allowed")
    actions: dict[str, ActionDefinition] = {}
    for action_index, raw_action in enumerate(require_list(root.get("actions", []), "actions")):
        field_name = f"actions[{action_index}]"
        action = require_mapping(raw_action, field_name)
        require_keys(action, {"key", "label", "description", "requiresConfirmation", "script"}, field_name)
        key = require_identifier(action.get("key"), f"{field_name}.key")
        if key in actions:
            raise ConfigurationError(f"Duplicate action key: {key}")
        if not isinstance(action.get("requiresConfirmation"), bool):
            raise ConfigurationError(f"{field_name}.requiresConfirmation must be true or false")
        actions[key] = ActionDefinition(key, require_text(action.get("label"), f"{field_name}.label"),
                                        require_text(action.get("description"), f"{field_name}.description", 500),
                                        action["requiresConfirmation"],
                                        parse_script(action.get("script"), script_root, f"{field_name}.script"))
    for section in sections:
        unknown_actions = set(section.action_keys) - set(actions)
        if unknown_actions:
            raise ConfigurationError(f"Section {section.identifier} references unknown actions: "
                                     f"{', '.join(sorted(unknown_actions))}")
    return RunnerConfiguration(
        project_slug=require_identifier(project.get("slug"), "project.slug"),
        project_name=require_text(project.get("name"), "project.name"),
        project_agent=require_text(project.get("agent"), "project.agent"),
        project_summary=require_text(project.get("summary"), "project.summary", 2_000),
        stale_after_minutes=require_integer(project.get("staleAfterMinutes"), "project.staleAfterMinutes", 5, 10_080),
        script_root=script_root,
        poll_seconds=require_integer(root.get("pollSeconds", 5), "pollSeconds", 1, 60),
        report_interval_seconds=require_integer(root.get("reportIntervalSeconds", 60), "reportIntervalSeconds", 5, 3_600),
        sections=sections,
        actions=actions,
    )


def execute_script(script: ScriptDefinition) -> CommandResult:
    process = subprocess.Popen(
        [str(script.path)], cwd=script.path.parent, env=SAFE_ENVIRONMENT,
        stdin=subprocess.DEVNULL, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        start_new_session=True,
    )
    assert process.stdout is not None
    output = bytearray()
    deadline = time.monotonic() + script.timeout_seconds
    timed_out = False
    output_limited = False
    while True:
        remaining = deadline - time.monotonic()
        if remaining <= 0:
            timed_out = True
            break
        ready, _, _ = select.select([process.stdout], [], [], remaining)
        if ready:
            chunk = os.read(process.stdout.fileno(), min(4_096, MAX_OUTPUT_BYTES + 1 - len(output)))
            if chunk:
                output.extend(chunk)
                if len(output) > MAX_OUTPUT_BYTES:
                    output_limited = True
                    break
            elif process.poll() is not None:
                break
        elif process.poll() is not None:
            break
    if timed_out or output_limited:
        try:
            os.killpg(process.pid, signal.SIGKILL)
        except ProcessLookupError:
            pass
    try:
        exit_code = process.wait(timeout=2)
    except subprocess.TimeoutExpired:
        os.killpg(process.pid, signal.SIGKILL)
        exit_code = process.wait()
    process.stdout.close()
    text = bytes(output[:MAX_OUTPUT_BYTES]).decode("utf-8", errors="replace")
    if output_limited:
        return CommandResult("FAILURE", text + "\n[output limit exceeded]")
    if timed_out:
        return CommandResult("FAILURE", text + "\n[timeout exceeded]")
    if exit_code != 0:
        return CommandResult("FAILURE", text)
    return CommandResult("SUCCESS", text)


def metric_widget(widget: WidgetDefinition, result: CommandResult) -> dict[str, Any]:
    value = result.output.strip() or "No output"
    if result.state != "SUCCESS":
        value = f"Check failed: {value}"
    return {"title": widget.title, "type": "METRIC", "value": value[:100]}


def structured_widget(widget: WidgetDefinition, result: CommandResult) -> dict[str, Any]:
    base = {"title": widget.title, "type": widget.widget_type}
    if result.state != "SUCCESS":
        return {"title": widget.title, "type": "METRIC", "value": "Check failed"}
    try:
        value = json.loads(result.output)
    except json.JSONDecodeError:
        return {"title": widget.title, "type": "METRIC", "value": "Invalid check output"}
    if widget.widget_type == "TABLE":
        if not isinstance(value, dict) or set(value) != {"columns", "rows"}:
            return {"title": widget.title, "type": "METRIC", "value": "Invalid table output"}
        columns, rows = value["columns"], value["rows"]
        if not isinstance(columns, list) or not 1 <= len(columns) <= 8 or not isinstance(rows, list) or len(rows) > MAX_TABLE_ROWS:
            return {"title": widget.title, "type": "METRIC", "value": "Table output exceeds bounds"}
        if any(not isinstance(column, str) or not column or len(column) > 100 for column in columns) or any(
                not isinstance(row, list) or len(row) != len(columns) or any(not isinstance(cell, str) or not cell or len(cell) > 500 for cell in row)
                for row in rows):
            return {"title": widget.title, "type": "METRIC", "value": "Invalid table cells"}
        base.update({"columns": columns, "rows": rows})
        return base
    points = value.get("points") if isinstance(value, dict) and set(value) == {"points"} else None
    if not isinstance(points, list) or not 1 <= len(points) <= MAX_LINE_POINTS or any(
            not isinstance(point, dict) or set(point) != {"label", "value"} or not isinstance(point["label"], str)
            or not point["label"] or len(point["label"]) > 100 or isinstance(point["value"], bool)
            or not isinstance(point["value"], (int, float)) or not abs(point["value"]) <= 1e12 for point in points):
        return {"title": widget.title, "type": "METRIC", "value": "Invalid line output"}
    base["points"] = points
    return base


def build_report(configuration: RunnerConfiguration, state: RunnerState) -> dict[str, Any]:
    sections = []
    has_failed_check = False
    for section in configuration.sections:
        widgets = [state.widgets.get(widget.identifier, {
            "title": widget.title, "type": "METRIC", "value": "Waiting for first check",
        }) for widget in section.widgets]
        has_failed_check = has_failed_check or any(
            widget.get("value", "").startswith(("Check failed", "Invalid", "Table output")) for widget in widgets)
        sections.append({"key": section.identifier, "title": section.title, "links": section.links,
                         "widgets": widgets, "actionKeys": section.action_keys})
    return {
        "name": configuration.project_name,
        "agent": configuration.project_agent,
        "status": "BLOCKED" if has_failed_check else "RUNNING",
        "summary": configuration.project_summary,
        "staleAfterMinutes": configuration.stale_after_minutes,
        "widgets": [],
        "sections": sections,
        "actions": [{"key": action.key, "label": action.title, "description": action.description,
                     "requiresConfirmation": action.confirmation}
                    for action in configuration.actions.values()],
    }


class RunnerClient:
    def __init__(self, website_url: str, token: str):
        self.base_url = website_url.rstrip("/")
        self.token = token

    def request(self, method: str, path: str, payload: dict[str, Any] | None = None) -> tuple[int, dict[str, Any] | None]:
        body = None if payload is None else json.dumps(payload, separators=(",", ":")).encode()
        request = urllib.request.Request(self.base_url + path, data=body, method=method, headers={
            "X-Runner-Token": self.token,
            "Content-Type": "application/json",
        })
        try:
            with urllib.request.urlopen(request, timeout=15) as response:
                response_body = response.read(MAX_OUTPUT_BYTES + 1)
                if len(response_body) > MAX_OUTPUT_BYTES:
                    raise RuntimeError("Runner API response exceeds output limit")
                if not response_body:
                    return response.status, None
                try:
                    return response.status, json.loads(response_body)
                except json.JSONDecodeError as exception:
                    raise RuntimeError("Runner API returned invalid JSON") from exception
        except urllib.error.HTTPError as exception:
            raise RuntimeError(f"Runner API returned HTTP {exception.code}") from exception
        except urllib.error.URLError as exception:
            raise RuntimeError("Runner API is unavailable") from exception

    def publish_report(self, configuration: RunnerConfiguration, report: dict[str, Any]) -> None:
        slug = urllib.parse.quote(configuration.project_slug, safe="")
        status, _ = self.request("PUT", f"/api/runner/projects/{slug}/report", report)
        if status != 204:
            raise RuntimeError(f"Unexpected report status: {status}")

    def claim_action(self, configuration: RunnerConfiguration) -> dict[str, Any] | None:
        slug = urllib.parse.quote(configuration.project_slug, safe="")
        status, body = self.request("POST", f"/api/runner/projects/{slug}/actions/claim")
        if status == 204:
            return None
        if status != 200 or not isinstance(body, dict):
            raise RuntimeError(f"Unexpected action claim status: {status}")
        return body

    def publish_action_result(self, configuration: RunnerConfiguration, request_id: Any, result: CommandResult) -> None:
        if not isinstance(request_id, str) or not request_id:
            raise RuntimeError("Action claim has invalid id")
        slug = urllib.parse.quote(configuration.project_slug, safe="")
        status, _ = self.request("PUT", f"/api/runner/projects/{slug}/actions/{request_id}/result", {
            "state": result.state,
            "output": truncate_utf8(result.output, MAX_OUTPUT_BYTES),
        })
        if status != 204:
            raise RuntimeError(f"Unexpected action result status: {status}")


class OperationsRunner:
    def __init__(self, configuration: RunnerConfiguration, client: RunnerClient):
        self.configuration = configuration
        self.client = client
        self.state = RunnerState()

    def refresh_checks(self, force: bool = False) -> None:
        now = time.monotonic()
        for section in self.configuration.sections:
            for widget in section.widgets:
                if not force and now < self.state.next_check_at.get(widget.identifier, 0):
                    continue
                result = execute_script(widget.check)
                self.state.widgets[widget.identifier] = metric_widget(widget, result) if widget.widget_type == "METRIC" else structured_widget(widget, result)
                self.state.next_check_at[widget.identifier] = now + (widget.check.interval_seconds or self.configuration.report_interval_seconds)

    def execute_claimed_action(self, claim: dict[str, Any]) -> None:
        if claim.get("projectSlug") != self.configuration.project_slug:
            raise RuntimeError("Action claim has the wrong project slug")
        key = claim.get("actionKey")
        action = self.configuration.actions.get(key)
        if action is None:
            self.client.publish_action_result(self.configuration, claim.get("id"), CommandResult("FAILURE", "Unknown local action key"))
            return
        self.client.publish_action_result(self.configuration, claim.get("id"), execute_script(action.script))

    def run_once(self) -> None:
        self.refresh_checks(force=True)
        self.client.publish_report(self.configuration, build_report(self.configuration, self.state))

    def run_forever(self) -> None:
        while True:
            try:
                self.refresh_checks()
                now = time.monotonic()
                if now - self.state.last_report_at >= self.configuration.report_interval_seconds:
                    self.client.publish_report(self.configuration, build_report(self.configuration, self.state))
                    self.state.last_report_at = now
                claim = self.client.claim_action(self.configuration)
                if claim is not None:
                    self.execute_claimed_action(claim)
            except (ConfigurationError, RuntimeError, OSError) as exception:
                logging.error("Operations runner iteration failed: %s", exception)
            time.sleep(self.configuration.poll_seconds)


def parse_arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", required=True, type=Path, help="server-owned YAML configuration")
    parser.add_argument("--once", action="store_true", help="publish checks and publish one report, then exit")
    parser.add_argument("--website-url", default=os.environ.get("WEBSITE_URL", "https://yunseong.dev"))
    return parser.parse_args()


def main() -> int:
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    arguments = parse_arguments()
    token = os.environ.get("RUNNER_TOKEN")
    if not token:
        logging.error("RUNNER_TOKEN is required")
        return 2
    try:
        configuration = load_configuration(arguments.config)
        runner = OperationsRunner(configuration, RunnerClient(arguments.website_url, token))
        if arguments.once:
            runner.run_once()
        else:
            runner.run_forever()
    except (ConfigurationError, RuntimeError, OSError) as exception:
        logging.error("Operations runner stopped: %s", exception)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
