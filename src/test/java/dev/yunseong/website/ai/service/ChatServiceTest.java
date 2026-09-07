package dev.yunseong.website.ai.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.yunseong.website.ai.domain.BlogAgent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    private static final String IP = "127.0.0.1";
    private static final String MESSAGE = "안녕하세요";

    @Mock
    private BlogAgent blogAgent;

    @Mock
    private ChatConversationService chatConversationService;

    private ChatService chatService;
    private ListAppender<ILoggingEvent> logAppender;
    private Logger chatServiceLogger;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(blogAgent, chatConversationService, new ObjectMapper());

        logAppender = new ListAppender<>();
        logAppender.start();
        chatServiceLogger = (Logger) LoggerFactory.getLogger(ChatService.class);
        chatServiceLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        chatServiceLogger.detachAppender(logAppender);
    }

    private static String textEvent(String content) {
        return "{\"type\":\"text\",\"content\":\"" + content + "\"}";
    }

    @Test
    void getChatResponse_ShouldSaveFullResponse_WhenStreamCompletes() {
        // Given
        when(blogAgent.prompt(MESSAGE, IP))
                .thenReturn(Flux.just(textEvent("Hello"), textEvent(" world")));

        // When
        List<String> events = chatService.getChatResponse(MESSAGE, IP).collectList().block();

        // Then
        assertThat(events).containsExactly(textEvent("Hello"), textEvent(" world"), "[DONE]");
        verify(chatConversationService).save(IP, MESSAGE, "Hello world", null);
    }

    @Test
    void getChatResponse_ShouldNotSave_WhenSubscriberCancels() {
        // Given
        when(blogAgent.prompt(MESSAGE, IP))
                .thenReturn(Flux.just(textEvent("Hello"), textEvent(" world")));

        // When: take(1) cancels the upstream after the first event
        List<String> events = chatService.getChatResponse(MESSAGE, IP).take(1).collectList().block();

        // Then
        assertThat(events).containsExactly(textEvent("Hello"));
        verify(chatConversationService, never()).save(anyString(), anyString(), anyString(), any());
    }

    @Test
    void getChatResponse_ShouldSavePartialResponseAndWarn_WhenStreamErrors() {
        // Given
        when(blogAgent.prompt(MESSAGE, IP))
                .thenReturn(Flux.just(textEvent("Hello")).concatWith(Flux.error(new IllegalStateException("boom"))));

        // When
        List<String> events = chatService.getChatResponse(MESSAGE, IP)
                .onErrorResume(e -> Flux.empty())
                .collectList()
                .block();

        // Then
        assertThat(events).containsExactly(textEvent("Hello"));
        verify(chatConversationService).save(IP, MESSAGE, "Hello", null);
        assertThat(logAppender.list)
                .anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.WARN);
                    assertThat(event.getFormattedMessage()).contains("partial");
                });
    }

    @Test
    void getChatResponse_ShouldNotBreakStream_WhenSaveFails() {
        // Given
        when(blogAgent.prompt(MESSAGE, IP)).thenReturn(Flux.just(textEvent("Hello")));
        org.mockito.Mockito.doThrow(new RuntimeException("db down"))
                .when(chatConversationService).save(anyString(), anyString(), anyString(), any());

        // When
        List<String> events = chatService.getChatResponse(MESSAGE, IP).collectList().block();

        // Then
        assertThat(events).containsExactly(textEvent("Hello"), "[DONE]");
    }
}
