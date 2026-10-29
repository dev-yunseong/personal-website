package dev.yunseong.website.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.yunseong.website.ai.domain.BlogAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final BlogAgent blogAgent;
    private final ChatConversationService chatConversationService;
    private final ObjectMapper objectMapper;

    public Flux<String> getChatResponse(String message, String ip) {
        StringBuilder responseBuilder = new StringBuilder();
        List<String> toolsUsed = new ArrayList<>();

        return blogAgent.prompt(message, ip)
                .doOnNext(event -> {
                    try {
                        JsonNode node = objectMapper.readTree(event);
                        String type = node.path("type").asText();
                        String content = node.path("content").asText();
                        if ("text".equals(type)) {
                            responseBuilder.append(content);
                        } else if ("tool".equals(type)) {
                            toolsUsed.add(content);
                        }
                    } catch (Exception e) {
                        log.debug("Skipping non-JSON stream event: {}", event, e);
                    }
                })
                .doOnError(this::logApiError)
                .concatWith(Flux.just("[DONE]"))
                .doFinally(signal -> {
                    if (signal == SignalType.CANCEL) {
                        log.debug("Chat stream cancelled before completion, not saving for ip={}", ip);
                        return;
                    }
                    if (signal == SignalType.ON_ERROR) {
                        log.warn("Saving partial chat response after stream error for ip={}", ip);
                    }
                    try {
                        String tools = toolsUsed.isEmpty() ? null : String.join(", ", toolsUsed);
                        chatConversationService.save(ip, message, responseBuilder.toString(), tools);
                    } catch (Exception e) {
                        log.error("Failed to save chat conversation for ip={}", ip, e);
                    }
                });
    }

    private void logApiError(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof WebClientResponseException response) {
                logApiError(response.getStatusCode().value(),
                        response.getHeaders().getFirst("x-request-id"), response.getResponseBodyAsString());
                return;
            }
            if (cause instanceof RestClientResponseException response) {
                logApiError(response.getStatusCode().value(),
                        response.getResponseHeaders() == null ? null
                                : response.getResponseHeaders().getFirst("x-request-id"),
                        response.getResponseBodyAsString());
                return;
            }
        }
    }

    private void logApiError(int status, String requestId, String body) {
        JsonNode error = null;
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root != null && root.path("error").isObject()) {
                error = root.path("error");
            }
        } catch (Exception ignored) {
            // JSON이 아닌 응답 본문은 기록하지 않고 HTTP 상태만 남긴다.
        }
        log.error("Chat API 요청 실패: status={}, request_id={}, message={}, type={}, param={}, code={}",
                status, logField(requestId),
                logField(error == null ? null : error.path("message").asText(null)),
                logField(error == null ? null : error.path("type").asText(null)),
                logField(error == null ? null : error.path("param").asText(null)),
                logField(error == null ? null : error.path("code").asText(null)));
    }

    private String logField(String value) {
        if (value == null) {
            return "null";
        }
        return objectMapper.getNodeFactory().textNode(value.substring(0, Math.min(value.length(), 2000)))
                .toString();
    }
}
