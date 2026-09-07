package dev.yunseong.website.ai.domain;

import dev.yunseong.website.ai.tool.BlogTools;
import dev.yunseong.website.ai.tool.ContentTools;
import dev.yunseong.website.ai.tool.CuratorMemoryTools;
import dev.yunseong.website.ai.tool.SendMessageTool;
import dev.yunseong.website.blog.service.MemoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.Disposable;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_SELF;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Two chat requests are in flight at the same time. Each one must see only its own tool events, and neither
 * one ending may terminate the other's stream. See issue #131.
 */
class BlogAgentToolEventIsolationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final String MESSAGE_A = "question A";
    private static final String MESSAGE_B = "question B";

    private final ToolEventPublisher toolEventPublisher = new ToolEventPublisher();
    private final SendMessageTool sendMessageTool = new SendMessageTool(toolEventPublisher);

    private final Map<String, Sinks.Many<String>> textSinks = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> toolContexts = new ConcurrentHashMap<>();

    private BlogAgent blogAgent;

    @BeforeEach
    void setUp() {
        // built before any stubbing starts: creating mocks inside a when(...) argument breaks Mockito
        ChatClient.ChatClientRequestSpec specA = requestSpecFor(MESSAGE_A);
        ChatClient.ChatClientRequestSpec specB = requestSpecFor(MESSAGE_B);

        ChatClient.Builder builder = mock(ChatClient.Builder.class, RETURNS_SELF);
        ChatClient chatClient = mock(ChatClient.class);
        when(builder.build()).thenReturn(chatClient);
        when(chatClient.mutate()).thenReturn(builder);
        when(chatClient.prompt(MESSAGE_A)).thenReturn(specA);
        when(chatClient.prompt(MESSAGE_B)).thenReturn(specB);

        MemoService memoService = mock(MemoService.class);
        when(memoService.findMemoWithoutVisibilityFilter(any())).thenReturn(Optional.empty());

        blogAgent = new BlogAgent(builder, mock(ChatMemory.class), mock(VectorStore.class),
                mock(BlogTools.class), mock(ContentTools.class), mock(CuratorMemoryTools.class),
                sendMessageTool, memoService, toolEventPublisher, null);
    }

    /** A request spec whose text stream is driven by the test, and which records the tool context it was given. */
    private ChatClient.ChatClientRequestSpec requestSpecFor(String message) {
        ChatClient.ChatClientRequestSpec spec = mock(ChatClient.ChatClientRequestSpec.class, RETURNS_SELF);
        when(spec.toolContext(any())).thenAnswer(invocation -> {
            toolContexts.put(message, invocation.getArgument(0));
            return spec;
        });

        Sinks.Many<String> textSink = Sinks.many().unicast().onBackpressureBuffer();
        textSinks.put(message, textSink);
        ChatClient.StreamResponseSpec streamSpec = mock(ChatClient.StreamResponseSpec.class);
        when(streamSpec.content()).thenReturn(textSink.asFlux());
        when(spec.stream()).thenReturn(streamSpec);
        return spec;
    }

    @Test
    void concurrentRequestsReceiveOnlyTheirOwnToolEvents() throws Exception {
        List<String> eventsA = new CopyOnWriteArrayList<>();
        List<String> eventsB = new CopyOnWriteArrayList<>();
        CountDownLatch doneA = new CountDownLatch(1);
        CountDownLatch doneB = new CountDownLatch(1);

        blogAgent.prompt(MESSAGE_A, "conv-A").subscribe(eventsA::add, error -> doneA.countDown(), doneA::countDown);
        blogAgent.prompt(MESSAGE_B, "conv-B").subscribe(eventsB::add, error -> doneB.countDown(), doneB::countDown);

        ToolContext contextA = new ToolContext(toolContexts.get(MESSAGE_A));
        ToolContext contextB = new ToolContext(toolContexts.get(MESSAGE_B));
        assertThat(contextA.getContext().get(ToolEventPublisher.REQUEST_ID_KEY))
                .isNotNull()
                .isNotEqualTo(contextB.getContext().get(ToolEventPublisher.REQUEST_ID_KEY));
        assertThat(toolEventPublisher.activeSinkCount()).isEqualTo(2);

        // both requests in flight: a tool call reaches only the request that triggered it
        sendMessageTool.sendMessage("from A", contextA);
        sendMessageTool.sendMessage("from B", contextB);

        assertThat(eventsA).containsExactly(
                StreamEvent.tool("send_message", "from A"), StreamEvent.message("from A"));
        assertThat(eventsB).containsExactly(
                StreamEvent.tool("send_message", "from B"), StreamEvent.message("from B"));

        // A finishing must not terminate B
        textSinks.get(MESSAGE_A).tryEmitComplete();
        assertThat(doneA.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)).isTrue();
        assertThat(doneB.getCount()).isEqualTo(1);
        assertThat(toolEventPublisher.activeSinkCount()).isEqualTo(1);

        sendMessageTool.sendMessage("from B again", contextB);
        assertThat(eventsB).hasSize(4).contains(StreamEvent.message("from B again"));

        // a late tool event for the finished request is dropped, never routed into another request
        sendMessageTool.sendMessage("late A", contextA);
        assertThat(eventsA).hasSize(2);

        textSinks.get(MESSAGE_B).tryEmitComplete();
        assertThat(doneB.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)).isTrue();
        assertThat(toolEventPublisher.activeSinkCount()).isZero();
    }

    @Test
    void failingRequestReleasesOnlyItsOwnSink() throws Exception {
        List<String> eventsB = new CopyOnWriteArrayList<>();
        CountDownLatch doneA = new CountDownLatch(1);
        CountDownLatch doneB = new CountDownLatch(1);

        blogAgent.prompt(MESSAGE_A, "conv-A").subscribe(event -> {}, error -> doneA.countDown(), doneA::countDown);
        blogAgent.prompt(MESSAGE_B, "conv-B").subscribe(eventsB::add, error -> doneB.countDown(), doneB::countDown);
        assertThat(toolEventPublisher.activeSinkCount()).isEqualTo(2);

        textSinks.get(MESSAGE_A).tryEmitError(new IllegalStateException("upstream blew up"));

        assertThat(doneA.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)).isTrue();
        assertThat(doneB.getCount()).isEqualTo(1);
        assertThat(toolEventPublisher.activeSinkCount()).isEqualTo(1);

        sendMessageTool.sendMessage("from B", new ToolContext(toolContexts.get(MESSAGE_B)));
        assertThat(eventsB).containsExactly(
                StreamEvent.tool("send_message", "from B"), StreamEvent.message("from B"));
    }

    @Test
    void cancelledRequestReleasesOnlyItsOwnSink() throws Exception {
        CountDownLatch doneB = new CountDownLatch(1);

        Disposable subscriptionA = blogAgent.prompt(MESSAGE_A, "conv-A").subscribe();
        blogAgent.prompt(MESSAGE_B, "conv-B").subscribe(event -> {}, error -> doneB.countDown(), doneB::countDown);
        assertThat(toolEventPublisher.activeSinkCount()).isEqualTo(2);

        subscriptionA.dispose();

        assertThat(toolEventPublisher.activeSinkCount()).isEqualTo(1);
        assertThat(doneB.getCount()).isEqualTo(1);

        textSinks.get(MESSAGE_B).tryEmitComplete();
        assertThat(doneB.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)).isTrue();
        assertThat(toolEventPublisher.activeSinkCount()).isZero();
    }
}
