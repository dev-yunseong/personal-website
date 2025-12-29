package dev.yunseong.website.ai.service;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import dev.yunseong.website.ai.domain.RagDocument;
import dev.yunseong.website.ai.domain.ResourceType;
import dev.yunseong.website.ai.repository.RagDocumentRepository;
import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final TextSplitter textSplitter;
    private final MarkdownReader markdownReader;
    private final RagDocumentRepository ragDocumentRepository;
    private final VectorStore vectorStore;

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

        List<Document> documents = docTuples.stream().map(docTuple -> {
            RagDocument savedRagDocument = ragDocumentRepository.save(docTuple.getT2());
            Document document = docTuple.getT1();
            document.getMetadata().put("ragDocumentId", savedRagDocument.getId());
            return document;
        }).toList();

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
