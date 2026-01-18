package dev.yunseong.website.blog.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.comparator.Comparators;

import dev.yunseong.website.blog.domain.MemoDirectory;
import dev.yunseong.website.blog.domain.MemoFile;
import dev.yunseong.website.blog.domain.MemoMeta;
import dev.yunseong.website.blog.repository.MemoFileRepository;
import dev.yunseong.website.blog.repository.MemoRepository;

@ExtendWith(MockitoExtension.class)
class MemoFileServiceTest {

    @Mock
    private MemoRepository memoRepository;

    @Mock
    private MemoFileRepository memoFileRepository;

    @InjectMocks
    private MemoFileService memoFileService;

    @BeforeEach
    void setUp() {
        memoFileService.root = null; // Ensure root is null before each test
    }

    @Test
    void initMemoFileSystem_shouldBuildCorrectTreeStructure() {
        // Given
        List<MemoMeta> mockMemoMetas = Arrays.asList(
                new MemoMeta(1L, "/category1/file1"),
                new MemoMeta(2L, "/category1/subcategoryA/file2"),
                new MemoMeta(3L, "/category2/file3"),
                new MemoMeta(4L, "/category1/file4")
        );
        when(memoFileRepository.findAllMemoMeta()).thenReturn(mockMemoMetas);

        // When
        memoFileService.initMemoFileSystem();

        // Then
        assertNotNull(memoFileService.root);
        assertEquals(2, memoFileService.root.getChildren().size()); // category1, category2

        // Check category1
        MemoDirectory category1 = memoFileService.root.getChildren().stream()
                .filter(d -> d.getName().equals("category1")).findFirst().orElse(null);
        assertNotNull(category1);
        assertEquals(2, category1.getFiles().size()); // file1.md, file4.md
        assertEquals(1, category1.getChildren().size()); // subcategoryA

        // Check file1.md
        MemoFile file1 = category1.getFiles().stream()
                .filter(f -> f.getName().equals("file1")).findFirst().orElse(null);
        assertNotNull(file1);
        assertEquals(1L, file1.getMemoId());
        assertEquals(category1, file1.getParent());

        // Check file4.md
        MemoFile file4 = category1.getFiles().stream()
                .filter(f -> f.getName().equals("file4")).findFirst().orElse(null);
        assertNotNull(file4);
        assertEquals(4L, file4.getMemoId());
        assertEquals(category1, file4.getParent());

        // Check subcategoryA
        MemoDirectory subcategoryA = category1.getChildren().stream()
                .filter(d -> d.getName().equals("subcategoryA")).findFirst().orElse(null);
        assertNotNull(subcategoryA);
        assertEquals(1, subcategoryA.getFiles().size()); // file2.md
        assertTrue(subcategoryA.getChildren().isEmpty());

        // Check file2.md
        MemoFile file2 = subcategoryA.getFiles().stream()
                .filter(f -> f.getName().equals("file2")).findFirst().orElse(null);
        assertNotNull(file2);
        assertEquals(2L, file2.getMemoId());
        assertEquals(subcategoryA, file2.getParent());

        // Check category2
        MemoDirectory category2 = memoFileService.root.getChildren().stream()
                .filter(d -> d.getName().equals("category2")).findFirst().orElse(null);
        assertNotNull(category2);
        assertEquals(1, category2.getFiles().size()); // file3.md
        assertTrue(category2.getChildren().isEmpty());

        // Check file3.md
        MemoFile file3 = category2.getFiles().stream()
                .filter(f -> f.getName().equals("file3")).findFirst().orElse(null);
        assertNotNull(file3);
        assertEquals(3L, file3.getMemoId());
        assertEquals(category2, file3.getParent());
    }

    @Test
    void initMemoFileSystem_shouldHandleEmptyMemoMetas() {
        // Given
        when(memoFileRepository.findAllMemoMeta()).thenReturn(Collections.emptyList());

        // When
        memoFileService.initMemoFileSystem();

        // Then
        assertNotNull(memoFileService.root);
        assertTrue(memoFileService.root.getChildren().isEmpty());
        assertTrue(memoFileService.root.getFiles().isEmpty());
    }

    @Test
    void initMemoFileSystem_shouldHandleMemosAtRootLevel() {
        // Given
        List<MemoMeta> mockMemoMetas = Arrays.asList(
                new MemoMeta(10L, "/root_file1"),
                new MemoMeta(11L, "/root_file2")
        );
        when(memoFileRepository.findAllMemoMeta()).thenReturn(mockMemoMetas);

        // When
        memoFileService.initMemoFileSystem();

        // Then
        assertNotNull(memoFileService.root);
        assertTrue(memoFileService.root.getChildren().isEmpty());
        assertEquals(2, memoFileService.root.getFiles().size());

        MemoFile rootFile1 = memoFileService.root.getFiles().stream()
                .filter(f -> f.getName().equals("root_file1")).findFirst().orElse(null);
        assertNotNull(rootFile1);
        assertEquals(10L, rootFile1.getMemoId());
        assertEquals(memoFileService.root, rootFile1.getParent());
    }

    @Test
    void initMemoFileSystem_shouldHandleComplexPaths() {
        // Given
        List<MemoMeta> mockMemoMetas = Arrays.asList(
                new MemoMeta(1L, "/a/b/c/file1"),
                new MemoMeta(2L, "/a/b/file2"),
                new MemoMeta(3L, "/x/y/file3")
        );
        when(memoFileRepository.findAllMemoMeta()).thenReturn(mockMemoMetas);

        // When
        memoFileService.initMemoFileSystem();

        // Then
        assertNotNull(memoFileService.root);
        assertEquals(2, memoFileService.root.getChildren().size()); // a, x

        MemoDirectory a = memoFileService.root.getChildren().stream().filter(d -> d.getName().equals("a")).findFirst().orElse(null);
        assertNotNull(a);
        MemoDirectory b = a.getChildren().stream().filter(d -> d.getName().equals("b")).findFirst().orElse(null);
        assertNotNull(b);
        MemoDirectory c = b.getChildren().stream().filter(d -> d.getName().equals("c")).findFirst().orElse(null);
        assertNotNull(c);

        assertEquals(1, c.getFiles().size());
        assertEquals("file1", c.getFiles().get(0).getName());
        assertEquals(1L, c.getFiles().get(0).getMemoId());
        assertEquals(c, c.getFiles().get(0).getParent());

        assertEquals(1, b.getFiles().size());
        assertEquals("file2", b.getFiles().get(0).getName());
        assertEquals(2L, b.getFiles().get(0).getMemoId());
        assertEquals(b, b.getFiles().get(0).getParent());

        MemoDirectory x = memoFileService.root.getChildren().stream().filter(d -> d.getName().equals("x")).findFirst().orElse(null);
        assertNotNull(x);
        MemoDirectory y = x.getChildren().stream().filter(d -> d.getName().equals("y")).findFirst().orElse(null);
        assertNotNull(y);

        assertEquals(1, y.getFiles().size());
        assertEquals("file3", y.getFiles().get(0).getName());
    }
}
