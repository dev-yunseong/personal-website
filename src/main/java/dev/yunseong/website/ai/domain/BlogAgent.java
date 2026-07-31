package dev.yunseong.website.ai.domain;

import java.util.List;
import java.util.Map;

import dev.yunseong.website.ai.tool.BlogTools;
import dev.yunseong.website.blog.service.MemoService;
import dev.yunseong.website.ai.tool.CuratorMemoryTools;
import dev.yunseong.website.ai.tool.SendMessageTool;
import dev.yunseong.website.ai.tool.ContentTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import dev.yunseong.website.ai.tool.DateTimeTools;
import reactor.core.publisher.Flux;

@Component
public class BlogAgent {

    private final ChatClient chatClient;
    private final MemoService memoService;
    private final ToolEventPublisher toolEventPublisher;
    private final static String DEFAULT_PROMPT = """
            # Role
            You are a highly competent Archive Curator for Yunsung's Blog. Your primary goal is to provide accurate, insightful, and strictly grounded responses based on the blog's contents.
            
            # Core Principles
            1. **Professional Tone**: Maintain a professional, helpful, and polite tone in all interactions.
            
            # Behavioral Guidelines (Proactive Engagement)
            1. **Clarification First**: If a user's inquiry is ambiguous or lacks sufficient detail, do not guess. You must ask clarifying questions to understand the user's intent.
            2. **Active Information Seeking**: If the current context is insufficient to provide a definitive answer, proactively request more details or specific topics from the user.
            3. **Accuracy over Completion**: It is better to ask for more information than to provide a generic or potentially incorrect response.
            
            # Information Retrieval Strategy
            1. **Primary Source (Blog First)**:
               - Always search Yunseong's Blog as the first source of truth.
               - If relevant content is found, base your response strictly on that content.
            
            2. **Secondary Source (MCP Fallback)**:
               - If the blog does not contain sufficient or relevant information, use MCP tools to retrieve additional data.
               - Clearly distinguish when information comes from outside the blog.
            
            3. **Transparency with Sources**:
               - When referencing blog or external content, always include the URL.
               - If multiple sources are used, list them clearly.
            
            # Task Execution
            - Analyze the user's request.
            - If information is missing or unclear, formulate a polite question to gather the necessary context.
            - Once sufficient information is available, provide a high-quality, grounded response.
            - Use Markdown (headings, bullet points, tables, bold text) to organize information logically and improve readability. Avoid dense blocks of text and ensure the response is easily scannable.
            """;

    private final static String BASE_SYSTEM_PROMPT = String.format("%s\n\n%s\n\n%s", DEFAULT_PROMPT, BlogTools.BLOG_TOOL_PROMPT, CuratorMemoryTools.CURATOR_MEMORY_PROMPT);

    public BlogAgent(ChatClient.Builder builder, ChatMemory chatMemory, VectorStore vectorStore, BlogTools blogTools, ContentTools contentTools, CuratorMemoryTools curatorMemoryTools, SendMessageTool sendMessageTool, MemoService memoService, ToolEventPublisher toolEventPublisher, @Nullable ToolCallbackProvider mcpToolCallbackProvider) {
        this.memoService = memoService;
        this.toolEventPublisher = toolEventPublisher;
        ChatClient pureChatClient = builder.build();

        ChatClient.Builder chatClientBuilder = builder.defaultAdvisors(List.of( // LLM 사이에서 intercept 한다.
                        new SimpleLoggerAdvisor(),
                        RetrievalAugmentationAdvisor.builder()
                                .queryTransformers(RewriteQueryTransformer.builder()
                                        .chatClientBuilder(pureChatClient.mutate())
                                        .build())
                                .queryExpander(MultiQueryExpander.builder()
                                        .chatClientBuilder(pureChatClient.mutate())
                                        .includeOriginal(false)
                                        .build())
                                .documentRetriever(VectorStoreDocumentRetriever.builder()
                                        .similarityThreshold(0.7)
                                        .topK(6)
                                        .vectorStore(vectorStore)
                                        .build())
                                .queryAugmenter(ContextualQueryAugmenter.builder()
                                        .allowEmptyContext(true)
                                        .build())
                                .build(), // RAG Advisor
                        PromptChatMemoryAdvisor.builder(chatMemory).build() // Chat Memory Advisor
                ))
                .defaultTools(new DateTimeTools(), blogTools, contentTools, curatorMemoryTools, sendMessageTool);

        if (mcpToolCallbackProvider != null) {
            chatClientBuilder = chatClientBuilder.defaultToolCallbacks(mcpToolCallbackProvider);
        }

        chatClient = chatClientBuilder.build();
    }

    private String buildSystemPrompt() {
        return memoService.findMemoWithoutVisibilityFilter(CuratorMemoryTools.AGENTS_MEMORY_PATH)
                .map(memo -> """
                        %s

                        # Curator AGENTS.md Memory
                        %s
                        """.formatted(BASE_SYSTEM_PROMPT, memo.getContent()))
                .orElse(BASE_SYSTEM_PROMPT);
    }

    public Flux<String> prompt(String message) {
        return prompt(message, "default");
    }

    public Flux<String> prompt(String message, String conversationId) {
        // Registered per subscription so an unsubscribed Flux never leaves a sink behind.
        return Flux.defer(() -> {
            ToolEventPublisher.Registration registration = toolEventPublisher.registerSink();

            Flux<String> textStream = chatClient.prompt(message)
                    .system(buildSystemPrompt())
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .toolContext(Map.of(
                            BlogTools.CONVERSATION_ID_KEY, conversationId,
                            ToolEventPublisher.REQUEST_ID_KEY, registration.requestId()))
                    .stream()
                    .content()
                    .map(StreamEvent::text)
                    // fires on complete, error and cancel, so the sink is always removed
                    .doFinally(signal -> toolEventPublisher.complete(registration.requestId()));

            return Flux.merge(textStream, registration.sink().asFlux());
        });
    }
}
