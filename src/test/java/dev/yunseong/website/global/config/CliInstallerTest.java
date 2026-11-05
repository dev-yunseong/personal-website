package dev.yunseong.website.global.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class CliInstallerTest {

    private static final Path INSTALLER = Path.of("src/main/resources/static/cli/install.sh");

    @TempDir
    Path temporaryDirectory;

    private Path home;
    private Path fakeBin;
    private Path payload;

    @BeforeEach
    void setUp() throws Exception {
        home = Files.createDirectory(temporaryDirectory.resolve("home with spaces"));
        fakeBin = Files.createDirectory(temporaryDirectory.resolve("fake-bin"));
        payload = temporaryDirectory.resolve("payload");
        Files.writeString(payload, "#!/bin/sh\nprintf 'version one\\n'\n");
        Path curl = fakeBin.resolve("curl");
        Files.writeString(curl, """
                #!/bin/sh
                set -eu
                destination=''
                source=''
                url=''
                while [ "$#" -gt 0 ]; do
                    case "$1" in
                        --output) destination=$2; shift ;;
                        https://yunseong.dev/cli/website) source=$TEST_PAYLOAD; url=$1 ;;
                        https://yunseong.dev/cli/website-project-report/SKILL.md) source=$TEST_SKILL; url=$1 ;;
                        https://yunseong.dev/cli/project-report.json) source=$TEST_EXAMPLE; url=$1 ;;
                    esac
                    shift
                done
                [ -n "$source" ]
                [ -n "$destination" ]
                cp "$source" "$destination"
                if [ -z "${TEST_FAIL_URL:-}" ] || [ "$TEST_FAIL_URL" = "$url" ]; then
                    exit "${TEST_CURL_STATUS:-0}"
                fi
                exit 0
                """);
        assertThat(curl.toFile().setExecutable(true)).isTrue();
    }

    @Test
    void installsInHomeWithSpacesAndUpdatesAtomically() throws Exception {
        Result initial = install(null, "0", INSTALLER);
        Path command = home.resolve(".local/bin/website");
        assertThat(initial.exitCode()).isZero();
        assertThat(Files.readAllBytes(command)).isEqualTo(Files.readAllBytes(payload));
        assertThat(Files.isExecutable(command)).isTrue();
        assertThat(initial.output()).contains("export PATH=\"$HOME/.local/bin:$PATH\"");
        assertThat(runCommand(command)).isEqualTo("version one\n");

        Files.writeString(payload, "#!/bin/sh\nprintf 'version two\\n'\n");
        assertThat(install(null, "0", INSTALLER).exitCode()).isZero();
        assertThat(runCommand(command)).isEqualTo("version two\n");
        assertNoTemporaryFiles(command.getParent());
    }

    @Test
    void failedDownloadPreservesExistingCommandAndCleansTemporaryFile() throws Exception {
        Path installDirectory = temporaryDirectory.resolve("custom bin");
        Files.createDirectory(installDirectory);
        Path command = installDirectory.resolve("website");
        Files.writeString(command, "previous command");

        assertThat(install(installDirectory, "22", INSTALLER).exitCode()).isNotZero();
        assertThat(Files.readString(command)).isEqualTo("previous command");
        assertNoTemporaryFiles(installDirectory);
    }

    @ParameterizedTest
    @ValueSource(strings = {"<html>not a command</html>\n", "#!/bin/sh\nif then\n", ""})
    void rejectsInvalidDownloadWithoutReplacingCommand(String content) throws Exception {
        assertThat(install(null, "0", INSTALLER).exitCode()).isZero();
        Path command = home.resolve(".local/bin/website");
        byte[] original = Files.readAllBytes(command);
        Files.writeString(payload, content);

        Result result = install(null, "0", INSTALLER);
        assertThat(result.exitCode()).isNotZero();
        assertThat(result.output()).contains("not a valid shell command");
        assertThat(Files.readAllBytes(command)).isEqualTo(original);
        assertNoTemporaryFiles(command.getParent());
    }

    @Test
    void overrideDirectoryIsInstalledAndReportedForPath() throws Exception {
        Path installDirectory = temporaryDirectory.resolve("custom bin");
        Result result = install(installDirectory, "0", INSTALLER);
        assertThat(result.exitCode()).isZero();
        assertThat(Files.readAllBytes(installDirectory.resolve("website")))
                .isEqualTo(Files.readAllBytes(payload));
        assertThat(result.output()).contains("Add this directory to PATH", installDirectory.toString());
    }

    @Test
    void truncatedInstallerDoesNotCreateInstallDirectory() throws Exception {
        String script = Files.readString(INSTALLER);
        Path truncated = temporaryDirectory.resolve("truncated.sh");
        Files.writeString(truncated, script.substring(0, script.indexOf("        curl --fail")));

        assertThat(install(null, "0", truncated).exitCode()).isNotZero();
        assertThat(home.resolve(".local")).doesNotExist();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://yunseong.dev/cli/website",
            "https://yunseong.dev/cli/website-project-report/SKILL.md",
            "https://yunseong.dev/cli/project-report.json"
    })
    void anyDownloadFailurePreservesCommandAndSupportFiles(String failedUrl) throws Exception {
        assertThat(install(null, "0", INSTALLER).exitCode()).isZero();
        Path directory = home.resolve(".local/bin");
        Path command = directory.resolve("website");
        Path skill = directory.resolve(".website/SKILL.md");
        Path example = directory.resolve(".website/project-report.json");
        Files.writeString(skill, "previous skill");
        Files.writeString(example, "previous example");
        byte[] previousCommand = Files.readAllBytes(command);
        Files.writeString(payload, "#!/bin/sh\nprintf 'updated\\n'\n");

        assertThat(install(null, "22", INSTALLER, failedUrl).exitCode()).isNotZero();
        assertThat(Files.readAllBytes(command)).isEqualTo(previousCommand);
        assertThat(Files.readString(skill)).isEqualTo("previous skill");
        assertThat(Files.readString(example)).isEqualTo("previous example");
        assertNoTemporaryFiles(directory);
    }

    @Test
    void installedCommandInstallsCompleteProjectSkillWithoutNetwork() throws Exception {
        Files.copy(Path.of("bin/website"), payload, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        assertThat(install(null, "0", INSTALLER).exitCode()).isZero();
        Path command = home.resolve(".local/bin/website");
        Path destination = temporaryDirectory.resolve("project skills");
        ProcessBuilder builder = new ProcessBuilder(command.toString(), "skills", "install", destination.toString())
                .redirectErrorStream(true);
        builder.environment().put("PATH", "/usr/bin:/bin");
        Process process = builder.start();
        assertThat(process.waitFor(10, TimeUnit.SECONDS)).isTrue();
        assertThat(process.exitValue()).as(new String(process.getInputStream().readAllBytes())).isZero();

        Path installedSkill = destination.resolve("website-project-report");
        assertThat(Files.readAllBytes(installedSkill.resolve("SKILL.md")))
                .isEqualTo(Files.readAllBytes(Path.of("skills/website-project-report/SKILL.md")));
        assertThat(Files.readAllBytes(installedSkill.resolve("assets/project-report.json")))
                .isEqualTo(Files.readAllBytes(Path.of("docs/examples/project-report.json")));
        assertThat(Files.readAllBytes(installedSkill.resolve("scripts/website")))
                .isEqualTo(Files.readAllBytes(Path.of("bin/website")));
    }

    @Test
    void commandReplacementFailureRestoresPreviousSupportDirectory() throws Exception {
        assertThat(install(null, "0", INSTALLER).exitCode()).isZero();
        Path directory = home.resolve(".local/bin");
        Path command = directory.resolve("website");
        Path skill = directory.resolve(".website/SKILL.md");
        Path example = directory.resolve(".website/project-report.json");
        Files.writeString(skill, "previous skill");
        Files.writeString(example, "previous example");
        byte[] previousCommand = Files.readAllBytes(command);
        Path failingMove = fakeBin.resolve("mv");
        Files.writeString(failingMove, """
                #!/bin/sh
                for argument do destination=$argument; done
                case "$destination" in
                    */website) exit 23 ;;
                esac
                exec /bin/mv "$@"
                """);
        assertThat(failingMove.toFile().setExecutable(true)).isTrue();

        assertThat(install(null, "0", INSTALLER).exitCode()).isNotZero();
        assertThat(Files.readAllBytes(command)).isEqualTo(previousCommand);
        assertThat(Files.readString(skill)).isEqualTo("previous skill");
        assertThat(Files.readString(example)).isEqualTo("previous example");
        assertNoTemporaryFiles(directory);
    }

    private Result install(Path installDirectory, String curlStatus, Path script) throws Exception {
        return install(installDirectory, curlStatus, script, "");
    }

    private Result install(Path installDirectory, String curlStatus, Path script, String failedUrl) throws Exception {
        ProcessBuilder builder = new ProcessBuilder("sh").redirectErrorStream(true);
        builder.redirectInput(script.toFile());
        builder.environment().put("HOME", home.toString());
        builder.environment().put("PATH", fakeBin + ":/usr/bin:/bin");
        builder.environment().put("TEST_PAYLOAD", payload.toString());
        builder.environment().put("TEST_CURL_STATUS", curlStatus);
        builder.environment().put("TEST_SKILL", Path.of("skills/website-project-report/SKILL.md").toAbsolutePath().toString());
        builder.environment().put("TEST_EXAMPLE", Path.of("docs/examples/project-report.json").toAbsolutePath().toString());
        builder.environment().put("TEST_FAIL_URL", failedUrl);
        builder.environment().remove("WEBSITE_INSTALL_DIR");
        if (installDirectory != null) {
            builder.environment().put("WEBSITE_INSTALL_DIR", installDirectory.toString());
        }
        Process process = builder.start();
        assertThat(process.waitFor(10, TimeUnit.SECONDS)).isTrue();
        return new Result(process.exitValue(), new String(process.getInputStream().readAllBytes()));
    }

    private String runCommand(Path command) throws Exception {
        Process process = new ProcessBuilder(command.toString()).redirectErrorStream(true).start();
        assertThat(process.waitFor(10, TimeUnit.SECONDS)).isTrue();
        assertThat(process.exitValue()).isZero();
        return new String(process.getInputStream().readAllBytes());
    }

    private void assertNoTemporaryFiles(Path directory) throws Exception {
        try (var files = Files.list(directory)) {
            assertThat(files.map(path -> path.getFileName().toString())
                    .filter(name -> name.startsWith(".website-install.")).toList()).isEmpty();
        }
    }

    private record Result(int exitCode, String output) {
    }
}
