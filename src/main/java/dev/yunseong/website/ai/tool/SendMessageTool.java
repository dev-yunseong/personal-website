package dev.yunseong.website.ai.tool;

import dev.yunseong.website.ai.domain.ToolEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendMessageTool {

    private final ToolEventPublisher toolEventPublisher;

    @Tool(description = "Send an intermediate message to the user during a long-running task. Use this to keep the user informed of progress before the final answer is ready.")
    public void sendMessage(String message) {
        log.info("sendMessage: {}", message);
        toolEventPublisher.emitTool("send_message", message);
        toolEventPublisher.emitMessage(message);
    }
}
