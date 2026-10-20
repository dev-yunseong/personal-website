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

    private CliResult runCliAt(String baseUrl, String... arguments) throws Exception {
        String[] command = new String[arguments.length + 2];
        command[0] = "sh";
        command[1] = "bin/website";
        System.arraycopy(arguments, 0, command, 2, arguments.length);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment().put("WEBSITE_URL", baseUrl);
        Process process = processBuilder.start();
        byte[] stdout = process.getInputStream().readAllBytes();
        byte[] stderr = process.getErrorStream().readAllBytes();
        int exitCode = process.waitFor();
        return new CliResult(exitCode, stdout, stderr);
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
