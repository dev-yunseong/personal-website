package dev.yunseong.website.briefing.controller;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class BriefingCliTest {

    private static final String TOKEN = "test-briefing-token";

    private HttpServer server;
    private String briefingUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        briefingUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void showRequestsTheSelectedBriefingAndPreservesAResponseWithoutTrailingNewline() throws Exception {
        byte[] markdown = "# 지난 뉴스\n\n본문".getBytes(StandardCharsets.UTF_8);
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        AtomicReference<String> token = new AtomicReference<>();
        server.createContext("/api/agent/briefings/news/2026-08-15", exchange -> {
            method.set(exchange.getRequestMethod());
            path.set(exchange.getRequestURI().getRawPath());
            token.set(exchange.getRequestHeaders().getFirst("X-Briefing-Token"));
            respond(exchange, 200, markdown);
        });

        CliResult result = runCli("show", "news", "2026-08-15");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).isEqualTo(markdown);
        assertThat(result.stderr()).isEmpty();
        assertThat(method.get()).isEqualTo("GET");
        assertThat(path.get()).isEqualTo("/api/agent/briefings/news/2026-08-15");
        assertThat(token.get()).isEqualTo(TOKEN);
    }

    @Test
    void showPreservesOneTrailingNewline() throws Exception {
        assertShowPreserves("# 지난 뉴스\n");
    }

    @Test
    void showPreservesMultipleTrailingNewlines() throws Exception {
        assertShowPreserves("# 지난 뉴스\n\n\n");
    }

    @Test
    void showKeepsStdoutCleanWhenTheServerReturnsNotFound() throws Exception {
        byte[] error = "No briefing published for kind 'news' on 2026-08-14.\n"
                .getBytes(StandardCharsets.UTF_8);
        server.createContext("/api/agent/briefings/news/2026-08-14", exchange ->
                respond(exchange, 404, error));

        CliResult result = runCli("show", "news", "2026-08-14");

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr())
                .startsWith(error)
                .endsWith("request failed with HTTP 404\n".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void showPropagatesAMalformedDateResponseOnStderr() throws Exception {
        byte[] error = "Date must be yyyy-MM-dd.\n".getBytes(StandardCharsets.UTF_8);
        server.createContext("/api/agent/briefings/news/yesterday", exchange ->
                respond(exchange, 400, error));

        CliResult result = runCli("show", "news", "yesterday");

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr())
                .startsWith(error)
                .endsWith("request failed with HTTP 400\n".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void showWithoutADateIsAUsageError() throws Exception {
        CliResult result = runCli("show", "news");

        assertUsageError(result);
    }

    @Test
    void showWithAnExtraArgumentIsAUsageError() throws Exception {
        CliResult result = runCli("show", "news", "2026-08-15", "extra");

        assertUsageError(result);
    }

    @Test
    void lastStillUsesTheLatestEndpointAndPreservesResponseBytes() throws Exception {
        byte[] markdown = "# 최신 뉴스\n\n본문\n\n".getBytes(StandardCharsets.UTF_8);
        server.createContext("/api/agent/briefings/news/latest", exchange ->
                respond(exchange, 200, markdown));

        CliResult result = runCli("last", "news");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).isEqualTo(markdown);
        assertThat(result.stderr()).isEmpty();
    }

    @Test
    void publishStillSendsTheMarkdownBodyAndPreservesTheResponse() throws Exception {
        byte[] markdown = "# 오늘의 뉴스\n\n본문\n\n".getBytes(StandardCharsets.UTF_8);
        byte[] response = "https://yunseong.dev/briefing?date=2026-08-15\n"
                .getBytes(StandardCharsets.UTF_8);
        AtomicReference<byte[]> requestBody = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();
        server.createContext("/api/agent/briefings/news", exchange -> {
            requestBody.set(exchange.getRequestBody().readAllBytes());
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            respond(exchange, 200, response);
        });

        CliResult result = runCliWithStdin(markdown, "publish", "news", "2026-08-15");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).isEqualTo(response);
        assertThat(result.stderr()).isEmpty();
        assertThat(requestBody.get()).isEqualTo(markdown);
        assertThat(contentType.get()).isEqualTo("text/plain; charset=utf-8");
    }

    @Test
    void kindsStillPreservesTheKindList() throws Exception {
        byte[] response = "jobs\nnews\n".getBytes(StandardCharsets.UTF_8);
        server.createContext("/api/agent/briefings/kinds", exchange ->
                respond(exchange, 200, response));

        CliResult result = runCli("kinds");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).isEqualTo(response);
        assertThat(result.stderr()).isEmpty();
    }

    @Test
    void kindsPreservesAnEmptySuccessfulResponse() throws Exception {
        server.createContext("/api/agent/briefings/kinds", exchange ->
                respond(exchange, 200, new byte[0]));

        CliResult result = runCli("kinds");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr()).isEmpty();
    }

    private void assertShowPreserves(String response) throws Exception {
        byte[] markdown = response.getBytes(StandardCharsets.UTF_8);
        server.createContext("/api/agent/briefings/news/2026-08-15", exchange ->
                respond(exchange, 200, markdown));

        CliResult result = runCli("show", "news", "2026-08-15");

        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).isEqualTo(markdown);
        assertThat(result.stderr()).isEmpty();
    }

    private static void assertUsageError(CliResult result) {
        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.stdout()).isEmpty();
        assertThat(new String(result.stderr(), StandardCharsets.UTF_8)).startsWith("usage:");
    }

    private CliResult runCli(String... arguments) throws Exception {
        return runCliWithStdin(new byte[0], arguments);
    }

    private CliResult runCliWithStdin(byte[] stdin, String... arguments) throws Exception {
        String[] command = new String[arguments.length + 2];
        command[0] = "sh";
        command[1] = "bin/briefing";
        System.arraycopy(arguments, 0, command, 2, arguments.length);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment().put("BRIEFING_URL", briefingUrl);
        processBuilder.environment().put("BRIEFING_TOKEN", TOKEN);
        Process process = processBuilder.start();
        process.getOutputStream().write(stdin);
        process.getOutputStream().close();
        byte[] stdout = process.getInputStream().readAllBytes();
        byte[] stderr = process.getErrorStream().readAllBytes();
        int exitCode = process.waitFor();
        return new CliResult(exitCode, stdout, stderr);
    }

    private static void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain;charset=UTF-8");
        exchange.sendResponseHeaders(status, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private record CliResult(int exitCode, byte[] stdout, byte[] stderr) {
    }
}
