package dev.yunseong.website.ai.service;

import dev.yunseong.website.ai.domain.RagDocument;
import dev.yunseong.website.ai.domain.ResourceType;
import dev.yunseong.website.ai.repository.RagDocumentRepository;
import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.repository.MemoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RagSyncService {

    private final TextSplitter textSplitter;
    private final MemoRepository memoRepository;
    private final MarkdownReader markdownReader;
    private final RagDocumentRepository ragDocumentRepository;
    private final VectorStore vectorStore;

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.DAYS)
    public void saveUnsavedMemos() {
        log.info("[RAG Sync] Start saving unsaved memos to vector store.");
        LocalDateTime lastUpdatedTime = getLastSavedTime();
        log.info("[RAG Sync] Last saved time: {}", lastUpdatedTime);
        List<Memo> unsavedMemos = memoRepository.findAllByUpdatedAtGreaterThan(lastUpdatedTime);

        if (unsavedMemos.isEmpty()) {
            log.info("[RAG Sync] No new memos to save.");
            return;
        }

        log.info("[RAG Sync] Found {} memos to save.", unsavedMemos.size());
        deleteByMemos(unsavedMemos);
        saveMemos(unsavedMemos);
        log.info("[RAG Sync] Finished saving unsaved memos to vector store.");
    }

    @Transactional(readOnly = true)
    protected LocalDateTime getLastSavedTime() {
        return ragDocumentRepository.findLastUpdatedAt().orElse(LocalDateTime.MIN);
    }

    private void deleteByMemos(List<Memo> memos) {
        log.info("[RAG Sync] Starting deletion of old RAG documents for {} memos.", memos.size());
        List<Long> memoIds = memos.stream().map(Memo::getId).toList();
        ResourceType type = ResourceType.MEMO;
        List<RagDocument> targets =
                ragDocumentRepository.findAllByResourceIdInAndResourceType(memoIds, type);

        if (targets.isEmpty()) {
            log.info("[RAG Sync] No old RAG documents found for deletion for {} memos.", memos.size());
            return;
        }

        log.info("[RAG Sync] Found {} old RAG documents to delete.", targets.size());
        ragDocumentRepository.deleteAllByResourceIdInAndResourceType(memoIds, type);

        FilterExpressionBuilder b = new FilterExpressionBuilder();
        Filter.Expression expression = b.in("ragDocumentId", targets.stream().map(RagDocument::getId).toArray()).build();
        vectorStore.delete(expression);
        log.info("[RAG Sync] Finished deletion of {} old RAG documents.", targets.size());
    }

    private void saveMemos(List<Memo> memos) {
        log.info("[RAG Sync] Starting saving new RAG documents for {} memos.", memos.size());
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

        log.info("[RAG Sync] Saving {} RagDocuments to the database.", ragDocuments.size());
        ragDocumentRepository.saveAll(ragDocuments);

        // Map saved RagDocument IDs back to the corresponding Documents
        List<Document> documents = IntStream.range(0, ragDocuments.size())
                .mapToObj(index -> {
                    RagDocument savedRagDocument = ragDocuments.get(index);
                    Document document = docTuples.get(index).getT1();
                    document.getMetadata().put("ragDocumentId", savedRagDocument.getId());
                    return document;
                })
                .toList();

        log.info("[RAG Sync] Adding {} Documents to the vector store.", documents.size());
        vectorStore.add(documents);
        log.info("[RAG Sync] Finished saving new RAG documents.");
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