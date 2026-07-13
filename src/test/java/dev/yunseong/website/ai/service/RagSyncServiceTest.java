package dev.yunseong.website.ai.service;

import dev.yunseong.website.ai.repository.RagDocumentRepository;
import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.repository.MemoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.vectorstore.VectorStore;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagSyncServiceTest {

    @Mock
    private TextSplitter textSplitter;

    @Mock
    private MemoRepository memoRepository;

    @Mock
    private MarkdownReader markdownReader;

    @Mock
    private RagDocumentRepository ragDocumentRepository;

    @Mock
    private VectorStore vectorStore;

    @Test
    void saveUnsavedMemos_ShouldDeletePrivateDocumentsButOnlyIndexPublicMemos() {
        // Given
        RagSyncService ragSyncService = new RagSyncService(
                textSplitter, memoRepository, markdownReader, ragDocumentRepository, vectorStore);

        Memo publicMemo = new Memo(1L, "/public/memo", "Public content", null, LocalDateTime.now());
        Memo privateMemo = new Memo(2L, "/private/curator/preferences", "Private memory", null, LocalDateTime.now());
        Document document = new Document("Public content");

        when(ragDocumentRepository.findLastUpdatedAt()).thenReturn(Optional.empty());
        when(memoRepository.findAllByUpdatedAtGreaterThan(LocalDateTime.of(1, 1, 1, 0, 0)))
                .thenReturn(List.of(publicMemo, privateMemo));
        when(ragDocumentRepository.findAllByResourceIdInAndResourceType(anyList(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());
        when(markdownReader.read("/public/memo", "Public content")).thenReturn(List.of(document));
        when(textSplitter.apply(List.of(document))).thenReturn(List.of(document));
        when(ragDocumentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ragSyncService.saveUnsavedMemos();

        // Then
        verify(markdownReader).read("/public/memo", "Public content");
        verify(markdownReader, never()).read("/private/curator/preferences", "Private memory");

        ArgumentCaptor<List<Document>> documentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(documentsCaptor.capture());
        assertThat(documentsCaptor.getValue()).hasSize(1);
    }
}
