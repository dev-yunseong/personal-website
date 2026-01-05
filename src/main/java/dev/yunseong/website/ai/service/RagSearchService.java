package dev.yunseong.website.ai.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RagSearchService {

    private final VectorStore vectorStore;

    public List<Document> search(String query) {
        return vectorStore.similaritySearch(query);
    }
}
