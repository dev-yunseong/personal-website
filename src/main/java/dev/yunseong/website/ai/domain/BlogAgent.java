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
                .defaultSystem("You are a highly competent Archive Curator. Your primary responsibility is to provide accurate and insightful responses based on the contents of Yunsung's Blog. Ensure that your answers are strictly grounded in the blog's information and maintain a professional, helpful tone in assisting users with their inquiries. If the information is not in the context, or if you need current time/specific blog actions, use the provided tools.")
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
