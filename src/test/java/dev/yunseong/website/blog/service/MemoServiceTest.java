package dev.yunseong.website.blog.service;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.repository.MemoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemoServiceTest {

    @Mock
    private MemoRepository memoRepository;

    @InjectMocks
    private MemoService memoService;

    private Memo javaMemo;
    private Memo javaSpringMemo;
    private Memo javaJpaMemo;

    @BeforeEach
    void setUp() {
        // Category-level memo
        javaMemo = new Memo(1L, "/java", "Java content", LocalDateTime.now(), LocalDateTime.now());
        
        // Child memos in the category
        javaSpringMemo = new Memo(2L, "/java/spring", "Spring content", LocalDateTime.now(), LocalDateTime.now());
        javaJpaMemo = new Memo(3L, "/java/jpa", "JPA content", LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void getMemosByCategory_ShouldIncludeCategoryMemoAndChildren() {
        // Given: A category with both a category-level memo and child memos
        String category = "/java";
        Pageable pageable = PageRequest.of(0, 10);
        
        // The repository should return both the category memo and its children
        List<Memo> memos = Arrays.asList(javaMemo, javaSpringMemo, javaJpaMemo);
        Page<Memo> page = new PageImpl<>(memos, pageable, memos.size());
        
        when(memoRepository.findAllByPath(eq("/java/"), any(PageRequest.class)))
            .thenReturn(page);

        // When: Getting memos by category
        Page<Memo> result = memoService.getMemos(category, pageable);

        // Then: Should include both the category memo and its children
        assertNotNull(result);
        assertEquals(3, result.getContent().size());
        assertThat(result.getContent()).contains(javaMemo, javaSpringMemo, javaJpaMemo);
        
        // Verify the path is normalized with trailing slash
        verify(memoRepository).findAllByPath(eq("/java/"), any(PageRequest.class));
    }

    @Test
    void getMemosByCategory_ShouldNormalizePathWithTrailingSlash() {
        // Given: A category path without trailing slash
        String category = "/java";
        Pageable pageable = PageRequest.of(0, 10);
        
        when(memoRepository.findAllByPath(eq("/java/"), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Arrays.asList()));

        // When: Getting memos by category
        memoService.getMemos(category, pageable);

        // Then: Should add trailing slash before querying
        verify(memoRepository).findAllByPath(eq("/java/"), any(PageRequest.class));
    }

    @Test
    void getMemosByCategory_WithTrailingSlash_ShouldNotAddAnotherSlash() {
        // Given: A category path with trailing slash
        String category = "/java/";
        Pageable pageable = PageRequest.of(0, 10);
        
        when(memoRepository.findAllByPath(eq("/java/"), any(PageRequest.class)))
            .thenReturn(new PageImpl<>(Arrays.asList()));

        // When: Getting memos by category
        memoService.getMemos(category, pageable);

        // Then: Should not add another trailing slash
        verify(memoRepository).findAllByPath(eq("/java/"), any(PageRequest.class));
    }

    @Test
    void saveMemo_ShouldSaveAndReturnId() {
        // Given
        when(memoRepository.save(any(Memo.class))).thenAnswer(invocation -> {
            Memo memo = invocation.getArgument(0);
            memo.setId(1L);
            return memo;
        });

        // When
        long id = memoService.saveMemo("Test", "Content");

        // Then
        assertEquals(1L, id);
        verify(memoRepository).save(any(Memo.class));
    }

    @Test
    void getMemoByName_ShouldReturnMemo() {
        // Given
        when(memoRepository.findByName("/java")).thenReturn(Optional.of(javaMemo));

        // When
        Memo result = memoService.getMemo("/java");

        // Then
        assertNotNull(result);
        assertEquals("/java", result.getName());
        verify(memoRepository).findByName("/java");
    }

    @Test
    void getMemoByName_WhenNotFound_ShouldThrowException() {
        // Given
        when(memoRepository.findByName("/notfound")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> memoService.getMemo("/notfound"));
        verify(memoRepository).findByName("/notfound");
    }

    @Test
    void getMemoById_ShouldReturnMemo() {
        // Given
        when(memoRepository.findById(1L)).thenReturn(Optional.of(javaMemo));

        // When
        Memo result = memoService.getMemo(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(memoRepository).findById(1L);
    }

    @Test
    void updateMemo_ShouldUpdateMemoTitleAndContent() {
        // Given
        when(memoRepository.findById(1L)).thenReturn(Optional.of(javaMemo));

        // When
        memoService.updateMemo(1L, "New Title", "New Content");

        // Then
        assertEquals("New Title", javaMemo.getName());
        assertEquals("New Content", javaMemo.getContent());
        verify(memoRepository).findById(1L);
    }
}
