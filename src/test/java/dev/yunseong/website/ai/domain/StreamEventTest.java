package dev.yunseong.website.ai.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StreamEventTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void text_ShouldProduceExactEventShape() {
        assertThat(StreamEvent.text("hello")).isEqualTo("{\"type\":\"text\",\"content\":\"hello\"}");
    }

    @Test
    void message_ShouldProduceExactEventShape() {
        assertThat(StreamEvent.message("hello")).isEqualTo("{\"type\":\"message\",\"content\":\"hello\"}");
    }

    @Test
    void tool_ShouldJoinToolNameAndArgs() {
        assertThat(StreamEvent.tool("searchBlog", "{\"q\":\"jvm\"}"))
                .isEqualTo("{\"type\":\"tool\",\"content\":\"searchBlog: {\\\"q\\\":\\\"jvm\\\"}\"}");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void tool_ShouldOmitSeparatorWhenArgsAreBlank(String args) {
        assertThat(StreamEvent.tool("listMemos", args))
                .isEqualTo("{\"type\":\"tool\",\"content\":\"listMemos\"}");
    }

    /**
     * The serialization failure branch is defensive: serializing a two-String record into an
     * in-memory writer has no failure path. These are the payloads that would realistically break
     * a hand-built JSON string, and none of them throws - each one survives a full round-trip.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "quote \" and backslash \\ and slash /",
            "line1\nline2\r\ttab",
            "control \u0000\u0001\u001f chars",
            "\uD83D\uDE80 emoji and \uD800\uDC00 non-BMP",
            "lone high surrogate \uD800 tail",
            "lone low surrogate \uDC00 tail",
            "한글 유니코드 \u00A0 nbsp",
            "</script><script>alert(1)</script>",
    })
    void serialize_ShouldRoundTripHostileContent(String content) throws Exception {
        String event = StreamEvent.text(content);

        JsonNode node = MAPPER.readTree(event);
        assertThat(node.path("type").asText()).isEqualTo("text");
        assertThat(node.path("content").asText()).isEqualTo(content);

        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("type", "text");
        expected.put("content", content);
        assertThat(event).isEqualTo(MAPPER.writeValueAsString(expected));
    }

    @Test
    void serialize_ShouldNeverEmitEmptyContentForNonEmptyInput() {
        String content = "\uD800 partial surrogate then \u0007 bell";

        assertThat(StreamEvent.text(content)).contains("\\u0007").doesNotContain("\"content\":\"\"");
    }
}
