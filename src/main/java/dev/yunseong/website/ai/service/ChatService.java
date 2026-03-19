package dev.yunseong.website.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.yunseong.website.ai.domain.BlogAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import org.springframework.stereotype.Service;

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
                .concatWith(Flux.just("[DONE]"))
                .doFinally(signal -> {
                    try {
                        String tools = toolsUsed.isEmpty() ? null : String.join(", ", toolsUsed);
                        chatConversationService.save(ip, message, responseBuilder.toString(), tools);
                    } catch (Exception e) {
                        log.error("Failed to save chat conversation for ip={}", ip, e);
                    }
                });
    }
}
