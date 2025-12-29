package dev.yunseong.website.ai.service;

import java.util.List;
import java.util.stream.IntStream;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.yunseong.website.ai.domain.RagDocument;
import dev.yunseong.website.ai.domain.ResourceType;
import dev.yunseong.website.ai.repository.RagDocumentRepository;
import dev.yunseong.website.blog.domain.Memo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EmbeddingService {

    private final TextSplitter textSplitter;
    private final MarkdownReader markdownReader;
    private final RagDocumentRepository ragDocumentRepository;
    private final VectorStore vectorStore;

    @Transactional(readOnly = true)
    public List<Document> search(String query) {
        return vectorStore.similaritySearch(query);
    }

    public void saveMemos(List<Memo> memos) {
        List<Tuple2<Document, RagDocument>> docTuples = memos.stream()
                .flatMap(memo -> {
                    var memoDocuments = markdownReader.read(memo.getName(), memo.getContent());
                    return textSplitter.apply(memoDocuments)
                            .stream().map(document ->
                                    Tuples.of(document, mapToRagDocument(memo.getId(), document)));
                }).toList();

        // Extract all RagDocuments and save them in a single batch operation
        List<RagDocument> ragDocuments = docTuples.stream()
                .map(Tuple2::getT2)
                .toList();

        List<RagDocument> savedRagDocuments = ragDocumentRepository.saveAll(ragDocuments);

        // Map saved RagDocument IDs back to the corresponding Documents
        List<Document> documents = IntStream.range(0, savedRagDocuments.size())
                .mapToObj(index -> {
                    RagDocument savedRagDocument = savedRagDocuments.get(index);
                    Document document = docTuples.get(index).getT1();
                    document.getMetadata().put("ragDocumentId", savedRagDocument.getId());
                    return document;
                })
                .toList();
        vectorStore.add(documents);
    }

    private RagDocument mapToRagDocument(long memoId, Document document) {
        log.debug(document.getText());
        return new RagDocument(
                ResourceType.MEMO,
                memoId,
                document.getText()
        );
    }
}
