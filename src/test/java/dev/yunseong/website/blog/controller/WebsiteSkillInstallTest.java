package dev.yunseong.website.blog.controller;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class WebsiteSkillInstallTest {

    private static final Path WEBSITE_CLI = Path.of("bin/website").toAbsolutePath();
    private static final Path CANONICAL_SKILL = Path.of("skills/website-project-report/SKILL.md");
    private static final Path CANONICAL_EXAMPLE = Path.of("docs/examples/project-report.json");

    @Test
    void installsDefaultAndCustomSkillsRootsWithBundledFiles(@TempDir Path tempProject) throws Exception {
        Path defaultRoot = tempProject.resolve(".agents/skills/website-project-report");
        CliResult defaultInstall = run(tempProject, "skills", "install");

        assertThat(defaultInstall.exitCode()).isZero();
        assertBundle(defaultRoot);

        Path customRoot = tempProject.resolve("skills with spaces");
        CliResult customInstall = run(tempProject, "skills", "install", customRoot.toString());

        assertThat(customInstall.exitCode()).isZero();
        assertBundle(customRoot.resolve("website-project-report"));
        assertThat(Files.exists(customRoot.resolve("website-project-report/.env"))).isFalse();
        try (var files = Files.walk(customRoot.resolve("website-project-report"))) {
            assertThat(files.map(Path::getFileName).map(Path::toString))
                    .doesNotContain(".env", "credentials", "secrets");
        }
    }

    @Test
    void refusesExistingDirectoryFileAndBrokenSymlinkWithoutChangingThem(@TempDir Path tempProject) throws Exception {
        Path root = tempProject.resolve("skills");
        Files.createDirectories(root);
        Path existingDirectory = root.resolve("website-project-report");
        Files.createDirectories(existingDirectory);
        Files.writeString(existingDirectory.resolve("keep.txt"), "keep");

        CliResult directoryResult = run(tempProject, "skills", "install", root.toString());
        assertThat(directoryResult.exitCode()).isNotZero();
        assertThat(Files.readString(existingDirectory.resolve("keep.txt"))).isEqualTo("keep");
        assertThat(Files.exists(existingDirectory.resolve("SKILL.md"))).isFalse();

        Path fileRoot = tempProject.resolve("file-skills");
        Files.createDirectories(fileRoot);
        Path existingFile = fileRoot.resolve("website-project-report");
        Files.writeString(existingFile, "keep file");
        CliResult fileResult = run(tempProject, "skills", "install", fileRoot.toString());
        assertThat(fileResult.exitCode()).isNotZero();
        assertThat(Files.readString(existingFile)).isEqualTo("keep file");

        Path linkRoot = tempProject.resolve("link-skills");
        Files.createDirectories(linkRoot);
        Path brokenLink = linkRoot.resolve("website-project-report");
        Files.createSymbolicLink(brokenLink, tempProject.resolve("missing-target"));
        CliResult linkResult = run(tempProject, "skills", "install", linkRoot.toString());
        assertThat(linkResult.exitCode()).isNotZero();
        assertThat(Files.isSymbolicLink(brokenLink)).isTrue();
        assertThat(Files.exists(brokenLink, LinkOption.NOFOLLOW_LINKS)).isTrue();
    }

    @Test
    void installedCliCanInstallItsOwnBundleFromAnotherWorkingDirectory(@TempDir Path tempProject) throws Exception {
        Path installed = tempProject.resolve("first/website-project-report");
        assertThat(run(tempProject, "skills", "install", tempProject.resolve("first").toString()).exitCode()).isZero();

        Path anotherRoot = tempProject.resolve("second root");
        CliResult result = runWithCli(tempProject, installed.resolve("scripts/website"),
                "skills", "install", anotherRoot.toString());

        assertThat(result.exitCode()).isZero();
        assertBundle(anotherRoot.resolve("website-project-report"));
    }

    @Test
    void bundledCliSubmitsProjectReportToLocalEndpoint(@TempDir Path tempProject) throws Exception {
        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<String> token = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        try {
            server.createContext("/api/agent/projects/portfolio-1", exchange -> {
                requestPath.set(exchange.getRequestURI().getPath());
                token.set(exchange.getRequestHeaders().getFirst("X-Briefing-Token"));
                body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
            });
            server.start();

            Path skillsRoot = tempProject.resolve("installed skills");
            assertThat(run(tempProject, "skills", "install", skillsRoot.toString()).exitCode()).isZero();
            Path bundledCli = skillsRoot.resolve("website-project-report/scripts/website").toAbsolutePath();
            Path bundledExample = skillsRoot.resolve("website-project-report/assets/project-report.json");
            CliResult result = runWithEnvironment(tempProject, bundledCli,
                    Map.of("WEBSITE_URL", "http://127.0.0.1:" + server.getAddress().getPort(),
                            "BRIEFING_TOKEN", "test-briefing-token"),
                    "project", "report", "portfolio-1", bundledExample.toString());

            assertThat(result.exitCode()).isZero();
            assertThat(requestPath.get()).isEqualTo("/api/agent/projects/portfolio-1");
            assertThat(token.get()).isEqualTo("test-briefing-token");
            assertThat(body.get()).isEqualTo(Files.readString(bundledExample));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsExtraArgumentsAndEmptySkillsPath(@TempDir Path tempProject) throws Exception {
        assertThat(run(tempProject, "skills", "install", "extra", "second").exitCode()).isNotZero();
        assertThat(run(tempProject, "skills", "install", "").exitCode()).isNotZero();
    }

    @Test
    void removesPartialInstallWhenCopyFailsAndPreservesOtherDirectory(@TempDir Path tempProject) throws Exception {
        Path root = tempProject.resolve("skills");
        Path existingDirectory = root.resolve("existing");
        Files.createDirectories(existingDirectory);
        Files.writeString(existingDirectory.resolve("keep.txt"), "keep");

        Path fakeBin = tempProject.resolve("fake-bin");
        Files.createDirectories(fakeBin);
        Path fakeCp = fakeBin.resolve("cp");
        Files.writeString(fakeCp, "#!/bin/sh\nexit 1\n");
        assertThat(fakeCp.toFile().setExecutable(true)).isTrue();

        ProcessBuilder builder = command(tempProject, WEBSITE_CLI, "skills", "install", root.toString());
        builder.environment().put("PATH", fakeBin + ":/usr/bin:/bin");
        CliResult result = execute(builder);

        assertThat(result.exitCode()).isNotZero();
        assertThat(Files.exists(root.resolve("website-project-report"))).isFalse();
        assertThat(Files.readString(existingDirectory.resolve("keep.txt"))).isEqualTo("keep");
    }

    private static void assertBundle(Path installed) throws IOException {
        assertThat(Files.readString(installed.resolve("SKILL.md"))).isEqualTo(Files.readString(CANONICAL_SKILL));
        assertThat(Files.readString(installed.resolve("scripts/website"))).isEqualTo(Files.readString(WEBSITE_CLI));
        assertThat(Files.isExecutable(installed.resolve("scripts/website"))).isTrue();
        assertThat(Files.readString(installed.resolve("assets/project-report.json")))
                .isEqualTo(Files.readString(CANONICAL_EXAMPLE));
    }

    private static CliResult run(Path workingDirectory, String... arguments) throws Exception {
        return runWithCli(workingDirectory, WEBSITE_CLI, arguments);
    }

    private static CliResult runWithCli(Path workingDirectory, Path cli, String... arguments) throws Exception {
        return execute(command(workingDirectory, cli, arguments));
    }

    private static CliResult runWithEnvironment(Path workingDirectory, Path cli,
                                                Map<String, String> environment, String... arguments) throws Exception {
        ProcessBuilder builder = command(workingDirectory, cli, arguments);
        builder.environment().putAll(environment);
        return execute(builder);
    }

    private static ProcessBuilder command(Path workingDirectory, Path cli, String... arguments) {
        var command = new java.util.ArrayList<String>();
        command.add("sh");
        command.add(cli.toAbsolutePath().toString());
        command.addAll(java.util.List.of(arguments));
        ProcessBuilder builder = new ProcessBuilder(command).directory(workingDirectory.toFile());
        builder.environment().remove("BRIEFING_TOKEN");
        builder.environment().remove("DRAFT_TOKEN");
        return builder;
    }

    private static CliResult execute(ProcessBuilder builder) throws Exception {
        Process process = builder.start();
        byte[] stdout = process.getInputStream().readAllBytes();
        byte[] stderr = process.getErrorStream().readAllBytes();
        return new CliResult(process.waitFor(), stdout, stderr);
    }

    private record CliResult(int exitCode, byte[] stdout, byte[] stderr) { }
}
