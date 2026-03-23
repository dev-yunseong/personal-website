package dev.yunseong.website.ai.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class StreamEvent {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private StreamEvent() {}

    public static String text(String content) {
        return serialize("text", content);
    }

    public static String tool(String toolName, String args) {
        String content = (args != null && !args.isEmpty()) ? toolName + ": " + args : toolName;
        return serialize("tool", content);
    }

    public static String message(String content) {
        return serialize("message", content);
    }

    private static String serialize(String type, String content) {
        try {
            return MAPPER.writeValueAsString(new EventPayload(type, content));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize stream event type='{}': {}", type, e.getMessage());
            return "{\"type\":\"" + type + "\",\"content\":\"\"}";
        }
    }

    private record EventPayload(String type, String content) {}
}
