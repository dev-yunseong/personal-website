package dev.yunseong.website.ai.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class ToolEventPublisher {

    private final AtomicReference<Sinks.Many<String>> currentSink = new AtomicReference<>();

    public Sinks.Many<String> registerSink() {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        currentSink.set(sink);
        return sink;
    }

    public void emitTool(String toolName, String args) {
        Sinks.Many<String> sink = currentSink.get();
        if (sink == null) {
            return;
        }
        Sinks.EmitResult result = sink.tryEmitNext(StreamEvent.tool(toolName, args));
        if (result.isFailure()) {
            log.warn("Failed to emit tool event for tool '{}': {}", toolName, result);
        }
    }

    public void complete() {
        Sinks.Many<String> sink = currentSink.getAndSet(null);
        if (sink != null) {
            sink.tryEmitComplete();
        }
    }
}
