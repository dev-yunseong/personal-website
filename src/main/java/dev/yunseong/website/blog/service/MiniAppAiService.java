package dev.yunseong.website.blog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiniAppAiService {

    private final ChatClient openAiChatClient;
    private final ObjectMapper objectMapper;

    private static final Duration FETCH_TIMEOUT = Duration.ofSeconds(10);

    // ~2000 tokens for gpt-4o-mini context, leaving room for system + response
    private static final int MAX_HTML_CHARS = 8000;

    private static final String GENERATION_PROMPT = """
            You are analyzing an HTML mini-app file to generate metadata.
            
            Based on the HTML content below, generate a concise title and a short description (1-2 sentences) for this mini-app.
            
            Rules:
            - Title: short, descriptive, no more than 50 characters
            - Description: 1-2 sentences describing what the app does, in Korean
            - Respond ONLY with valid JSON in this exact format, nothing else:
            {"title": "...", "description": "..."}
            
            HTML content:
            %s
            """;

    public record MiniAppMetadata(String title, String description) {}

    public MiniAppMetadata generateMetadataFromUrl(String url) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(FETCH_TIMEOUT)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(FETCH_TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Failed to fetch URL: HTTP " + response.statusCode());
        }
        return generateMetadata(response.body());
    }

    public MiniAppMetadata generateMetadata(String htmlContent) {
        String truncated = htmlContent.length() > MAX_HTML_CHARS
                ? htmlContent.substring(0, MAX_HTML_CHARS)
                : htmlContent;

        String response = openAiChatClient.prompt()
                .user(GENERATION_PROMPT.formatted(truncated))
                .call()
                .content();

        return parseMetadata(response);
    }

    private MiniAppMetadata parseMetadata(String json) {
        if (json == null || json.isBlank()) {
            return new MiniAppMetadata("Mini App", "");
        }
        // Strip markdown code block if the model wrapped output in ```json ... ```
        String cleaned = json.trim();
        if (cleaned.startsWith("```")) {
            int start = cleaned.indexOf('\n');
            int end = cleaned.lastIndexOf("```");
            if (start >= 0 && end > start) {
                cleaned = cleaned.substring(start + 1, end).trim();
            }
        }
        try {
            MiniAppMetadata result = objectMapper.readValue(cleaned, MiniAppMetadata.class);
            return new MiniAppMetadata(
                    result.title() != null ? result.title() : "Mini App",
                    result.description() != null ? result.description() : ""
            );
        } catch (Exception e) {
            log.warn("Failed to parse AI metadata response: {}", json, e);
            return new MiniAppMetadata("Mini App", "");
        }
    }
}
