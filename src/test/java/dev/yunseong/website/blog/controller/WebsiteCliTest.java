package dev.yunseong.website.blog.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class WebsiteCliTest {

    private HttpServer server;
    private String websiteUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        websiteUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void listPassesFiltersAndPreservesJsonResponse() throws Exception {
        byte[] response = "{\"items\":[],\"page\":2,\"limit\":25,\"hasNext\":false}\n"
                .getBytes(StandardCharsets.UTF_8);
        AtomicReference<Map<String, String>> query = new AtomicReference<>();
        server.createContext("/api/public/memos", exchange -> {
            query.set(parseQuery(exchange));
            respond(exchange, 200, "application/json", response);
        });

        CliResult result = runCli(
                "memo", "list",
                "--updated-after", "2026-08-15T10:00:00",
                "--page", "2",
                "--limit", "25");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).isEqualTo(response);
        assertThat(result.stderr()).isEmpty();
        assertThat(query.get()).containsExactly(
                Map.entry("updatedAfter", "2026-08-15T10:00:00"),
                Map.entry("page", "2"),
                Map.entry("limit", "25"));
    }

    @Test
    void readPreservesMarkdownIncludingTrailingNewlines() throws Exception {
        byte[] markdown = "# 메모\n\n```text\n공백  유지\n```\n\n\n"
                .getBytes(StandardCharsets.UTF_8);
        server.createContext("/api/public/memos/42/content", exchange ->
                respond(exchange, 200, "text/markdown;charset=UTF-8", markdown));

        CliResult result = runCli("memo", "read", "42");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).isEqualTo(markdown);
        assertThat(result.stderr()).isEmpty();
    }

    @Test
    void httpFailureKeepsStdoutCleanAndReportsBody() throws Exception {
        byte[] error = "Memo Not Found\n".getBytes(StandardCharsets.UTF_8);
        server.createContext("/api/public/memos/404/content", exchange ->
                respond(exchange, 404, "text/plain;charset=UTF-8", error));

        CliResult result = runCli("memo", "read", "404");

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr())
                .startsWith(error)
                .contains("request failed with HTTP 404\n".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void connectionFailureExitsNonZero() throws Exception {
        CliResult result = runCliAt("http://127.0.0.1:1", "memo", "list");

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.stdout()).isEmpty();
        assertThat(new String(result.stderr(), StandardCharsets.UTF_8)).contains("curl:");
    }

    @Test
    void invalidArgumentsExitWithUsageError() throws Exception {
        CliResult result = runCli("memo", "read", "not-an-id");

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stdout()).isEmpty();
        assertThat(new String(result.stderr(), StandardCharsets.UTF_8)).startsWith("usage:");
    }

    @Test
    void emptyUpdatedAfterIsUsageError() throws Exception {
        CliResult result = runCli("memo", "list", "--updated-after", "");

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stdout()).isEmpty();
        assertThat(new String(result.stderr(), StandardCharsets.UTF_8)).startsWith("usage:");
    }

    private CliResult runCli(String... arguments) throws Exception {
        return runCliAt(websiteUrl, arguments);
    }

    @Test
    void draftSubmitsUtf8FileToPrivateIntakeOnly() throws Exception {
        String markdown = "# 한글\n\n+ & = %\n\n";
        AtomicReference<String> received = new AtomicReference<>();
        AtomicReference<String> token = new AtomicReference<>();
        server.createContext("/api/agent/drafts", exchange -> {
            token.set(exchange.getRequestHeaders().getFirst("X-Draft-Token"));
            received.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, "application/json", "{}".getBytes(StandardCharsets.UTF_8));
        });
        Path file = Files.createTempFile("draft content ", ".md");
        try {
            Files.writeString(file, markdown);
            CliResult result = runCli("memo", "draft", "/notes/한글", file.toString());
            assertThat(result.exitCode()).isZero();
            assertThat(token.get()).isEqualTo("test-draft-token");
            assertThat(decode(received.get())).isEqualTo("target=/notes/한글&content=" + markdown);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private CliResult runCliAt(String baseUrl, String... arguments) throws Exception {
        return runCliWithInput(baseUrl, null, arguments);
    }

    @Test
    void draftAcceptsStdinWithoutTrimmingTrailingNewlines() throws Exception {
        AtomicReference<String> received = new AtomicReference<>();
        server.createContext("/api/agent/drafts", exchange -> {
            received.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, "application/json", "{}".getBytes(StandardCharsets.UTF_8));
        });
        String body = "# stdin 한글\n\n";
        CliResult result = runCliWithInput(websiteUrl, body.getBytes(StandardCharsets.UTF_8),
                "memo", "draft", "notes/stdin", "-");
        assertThat(result.exitCode()).isZero();
        assertThat(decode(received.get())).isEqualTo("target=notes/stdin&content=" + body);
    }

    private CliResult runCliWithInput(String baseUrl, byte[] input, String... arguments) throws Exception {
        boolean withToken = Arrays.asList(arguments).contains("draft") || Arrays.asList(arguments).contains("--draft");
        return runCliConfigured(baseUrl, input, withToken, arguments);
    }

    private CliResult runCliConfigured(String baseUrl, byte[] input, boolean withToken, String... arguments) throws Exception {
        String[] command = new String[arguments.length + 2];
        command[0] = "sh";
        command[1] = "bin/website";
        System.arraycopy(arguments, 0, command, 2, arguments.length);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment().put("WEBSITE_URL", baseUrl);
        processBuilder.environment().remove("DRAFT_TOKEN");
        if (withToken) {
            processBuilder.environment().put("DRAFT_TOKEN", "test-draft-token");
        }
        Process process = processBuilder.start();
        if (input != null) process.getOutputStream().write(input);
        process.getOutputStream().close();
        byte[] stdout = process.getInputStream().readAllBytes();
        byte[] stderr = process.getErrorStream().readAllBytes();
        int exitCode = process.waitFor();
        return new CliResult(exitCode, stdout, stderr);
    }

    @Test
    void treeForwardsScopeAndPreservesTreeOutput() throws Exception {
        String tree = "/노트/\n└── 글 [id=42]\n";
        AtomicReference<Map<String, String>> query = new AtomicReference<>();
        server.createContext("/api/public/memos/tree", exchange -> {
            query.set(parseQuery(exchange));
            respond(exchange, 200, "text/plain", tree.getBytes(StandardCharsets.UTF_8));
        });
        CliResult result = runCli("memo", "tree", "/노트");
        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).isEqualTo(tree.getBytes(StandardCharsets.UTF_8));
        assertThat(query.get()).containsEntry("path", "/노트");
        assertThat(runCli("memo", "tree").exitCode()).isZero();
        assertThat(query.get()).containsEntry("path", "/");
    }

    @Test
    void categoryOptionIsEncodedAsLiteralPath() throws Exception {
        AtomicReference<Map<String, String>> query = new AtomicReference<>();
        server.createContext("/api/public/memos", exchange -> {
            query.set(parseQuery(exchange));
            respond(exchange, 200, "application/json", "{}".getBytes(StandardCharsets.UTF_8));
        });
        assertThat(runCli("memo", "list", "--category", "/한글/a_% &", "--limit", "5").exitCode()).isZero();
        assertThat(query.get()).containsEntry("category", "/한글/a_% &").containsEntry("limit", "5");
    }

    @Test
    void tokenEnablesDraftAwareTreeAndList() throws Exception {
        AtomicReference<String> token = new AtomicReference<>();
        server.createContext("/api/agent/drafts/", exchange -> {
            token.set(exchange.getRequestHeaders().getFirst("X-Draft-Token"));
            respond(exchange, 200, "text/plain", "draft result\n".getBytes(StandardCharsets.UTF_8));
        });
        for (String command : new String[]{"tree", "list"}) {
            CliResult result = runCliConfigured(websiteUrl, null, true, "memo", command);
            assertThat(result.exitCode()).isZero();
            assertThat(token.get()).isEqualTo("test-draft-token");
            assertThat(new String(result.stdout(), StandardCharsets.UTF_8)).isEqualTo("draft result\n");
        }
    }

    @Test
    void draftReadSelectsPublishedIdOrTargetWithoutExposingStoragePath() throws Exception {
        AtomicReference<Map<String, String>> query = new AtomicReference<>();
        String body = "# 초안\n\n";
        server.createContext("/api/agent/drafts/content", exchange -> {
            query.set(parseQuery(exchange));
            respond(exchange, 200, "text/markdown", body.getBytes(StandardCharsets.UTF_8));
        });
        assertThat(runCli("memo", "read", "42", "--draft").stdout()).isEqualTo(body.getBytes(StandardCharsets.UTF_8));
        assertThat(query.get()).containsExactly(Map.entry("memoId", "42"));
        assertThat(runCli("memo", "read", "notes/new", "--draft").exitCode()).isZero();
        assertThat(query.get()).containsExactly(Map.entry("target", "notes/new"));
        assertThat(runCliConfigured(websiteUrl, null, false, "memo", "read", "42", "--draft").exitCode()).isEqualTo(2);
    }

    private static Map<String, String> parseQuery(HttpExchange exchange) {
        Map<String, String> parameters = new LinkedHashMap<>();
        String rawQuery = exchange.getRequestURI().getRawQuery();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return parameters;
        }
        Arrays.stream(rawQuery.split("&"))
                .map(pair -> pair.split("=", 2))
                .forEach(pair -> parameters.put(
                        decode(pair[0]),
                        pair.length == 2 ? decode(pair[1]) : ""));
        return parameters;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static void respond(
            HttpExchange exchange,
            int status,
            String contentType,
            byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private record CliResult(int exitCode, byte[] stdout, byte[] stderr) {
    }
}
