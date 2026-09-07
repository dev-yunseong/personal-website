package dev.yunseong.website.ai.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Routes tool events to the sink of the request that triggered the tool call.
 * <p>
 * The singleton holds no "current" sink: every {@link #registerSink()} call gets its own entry keyed by a
 * request id, and tools find their sink through the {@link ToolContext} Spring AI hands to the tool method.
 */
@Slf4j
@Component
public class ToolEventPublisher {

    public static final String REQUEST_ID_KEY = "toolEventRequestId";

    private final Map<String, Sinks.Many<String>> sinksByRequestId = new ConcurrentHashMap<>();

    public Registration registerSink() {
        String requestId = UUID.randomUUID().toString();
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        sinksByRequestId.put(requestId, sink);
        return new Registration(requestId, sink);
    }

    public void emitTool(@Nullable ToolContext toolContext, String toolName, String args) {
        Sinks.Many<String> sink = sinkOf(toolContext);
        if (sink == null) {
            return;
        }
        Sinks.EmitResult result = sink.tryEmitNext(StreamEvent.tool(toolName, args));
        if (result.isFailure()) {
            log.warn("Failed to emit tool event for tool '{}': {}", toolName, result);
        }
    }

    public void emitMessage(@Nullable ToolContext toolContext, String content) {
        Sinks.Many<String> sink = sinkOf(toolContext);
        if (sink == null) {
            return;
        }
        Sinks.EmitResult result = sink.tryEmitNext(StreamEvent.message(content));
        if (result.isFailure()) {
            log.warn("Failed to emit message event: {}", result);
        }
    }

    /** Completes and removes the sink. Must run on every terminal signal, including error and cancel. */
    public void complete(String requestId) {
        Sinks.Many<String> sink = sinksByRequestId.remove(requestId);
        if (sink != null) {
            sink.tryEmitComplete();
        }
    }

    /** Visible for testing: number of sinks still registered, i.e. requests still in flight. */
    int activeSinkCount() {
        return sinksByRequestId.size();
    }

    @Nullable
    private Sinks.Many<String> sinkOf(@Nullable ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }
        Object requestId = toolContext.getContext().get(REQUEST_ID_KEY);
        return requestId == null ? null : sinksByRequestId.get(requestId.toString());
    }

    public record Registration(String requestId, Sinks.Many<String> sink) {}
}
