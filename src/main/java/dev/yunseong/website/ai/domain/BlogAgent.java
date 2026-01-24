package dev.yunseong.website.ai.domain;

import java.util.List;

import dev.yunseong.website.ai.tool.BlogTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import dev.yunseong.website.ai.tool.DateTimeTools;
import reactor.core.publisher.Flux;

@Component
public class BlogAgent {

    private final ChatClient chatClient;
    private final static String DEFAULT_PROMPT = """
            # Role
            You are a highly competent Archive Curator for Yunsung's Blog. Your primary goal is to provide accurate, insightful, and strictly grounded responses based on the blog's contents.
            
            # Core Principles
            1. **Strict Grounding**: Every answer must be based on the provided blog information. Do not hallucinate or use external knowledge that contradicts the blog's context.
            2. **Professional Tone**: Maintain a professional, helpful, and polite tone in all interactions.
            
            # Behavioral Guidelines (Proactive Engagement)
            1. **Clarification First**: If a user's inquiry is ambiguous or lacks sufficient detail, do not guess. You must ask clarifying questions to understand the user's intent.
            2. **Active Information Seeking**: If the current context is insufficient to provide a definitive answer, proactively request more details or specific topics from the user.
            3. **Accuracy over Completion**: It is better to ask for more information than to provide a generic or potentially incorrect response.
            
            # Task Execution
            - Analyze the user's request.
            - If information is missing or unclear, formulate a polite question to gather the necessary context.
            - Once sufficient information is available, provide a high-quality, grounded response.
            """;

    public BlogAgent(ChatClient.Builder builder, ChatMemory chatMemory, VectorStore vectorStore, BlogTools blogTools) {
        ChatClient pureChatClient = builder.build();

        chatClient = builder.defaultAdvisors(List.of( // LLM 사이에서 intercept 한다.
                        new SimpleLoggerAdvisor(),
                        PromptChatMemoryAdvisor.builder(chatMemory).build(), // Chat Memory Advisor
                        RetrievalAugmentationAdvisor.builder()
                                .queryTransformers(RewriteQueryTransformer.builder()
                                        .chatClientBuilder(pureChatClient.mutate())
                                        .build())
                                .queryExpander(MultiQueryExpander.builder()
                                        .chatClientBuilder(pureChatClient.mutate())
                                        .includeOriginal(false)
                                        .build())
                                .documentRetriever(VectorStoreDocumentRetriever.builder()
                                        .similarityThreshold(0.2)
                                        .topK(6)
                                        .vectorStore(vectorStore)
                                        .build())
                                .queryAugmenter(ContextualQueryAugmenter.builder()
                                        .allowEmptyContext(true)
                                        .build())
                                .build() // RAG Advisor
                ))
                .defaultSystem(DEFAULT_PROMPT)
                .defaultSystem(BlogTools.BLOG_TOOL_PROMPT)
                .defaultTools(new DateTimeTools(), blogTools)
                .build();
    }

    public Flux<String> prompt(String message) {
        return prompt(message, "default");
    }
    public Flux<String> prompt(String message, String conversationId) {
        return chatClient.prompt(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }
}
