package dev.yunseong.website.ai.domain;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BlogAgentRetrievalTest {

    @Test
    void failSoft_ShouldReturnEmptyContextWhenRetrievalFails() {
        DocumentRetriever noEmbeddingEndpoint = query -> {
            throw new RuntimeException("404 Not Found on /v1/embeddings");
        };

        List<Document> documents = BlogAgent.failSoft(noEmbeddingEndpoint).retrieve(new Query("누구세요?"));

        assertThat(documents).isEmpty();
    }

    @Test
    void failSoft_ShouldPassRetrievedDocumentsThrough() {
        Document document = new Document("blog content");

        List<Document> documents = BlogAgent.failSoft(query -> List.of(document)).retrieve(new Query("누구세요?"));

        assertThat(documents).containsExactly(document);
    }
}
