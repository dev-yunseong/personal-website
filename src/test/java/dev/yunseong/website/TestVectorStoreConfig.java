package dev.yunseong.website;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Stands in for the pgvector store while the test profile runs on H2.
 *
 * <p>Component-scanned like production configuration because it lives under the
 * application package, and confined to the {@code test} profile so it can never
 * reach a running server. Nothing here embeds anything: the tests that load a
 * context exercise routes and security, not retrieval.
 */
@Profile("test")
@Configuration
public class TestVectorStoreConfig {

    @Bean
    VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
