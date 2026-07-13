package dev.yunseong.website.blog.controller;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.service.CategoryService;
import dev.yunseong.website.blog.service.MemoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.ui.Model;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicMemoControllerTest {

    @Mock
    private MemoService memoService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private Model model;

    @InjectMocks
    private PublicMemoController publicMemoController;

    @Test
    void blog_LoadsMemosForAllCategories() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Memo> memos = new PageImpl<>(Collections.emptyList());
        when(memoService.getPublicMemos(pageable)).thenReturn(memos);
        when(categoryService.getPublicCategoryTree()).thenReturn(Collections.emptyList());

        // When
        String result = publicMemoController.blog(model, null, pageable, null);

        // Then
        assertEquals("blog", result);
        verify(model).addAttribute("memos", memos);
        verify(model).addAttribute("categoryTree", Collections.emptyList());
        verify(model).addAttribute("selectedCategory", null);
    }

    @Test
    void blog_LoadsMemosForSpecificCategory() {
        // Given
        String category = "Java";
        Pageable pageable = PageRequest.of(0, 10);
        Page<Memo> memos = new PageImpl<>(Collections.emptyList());
        when(memoService.getPublicMemos(category, pageable)).thenReturn(memos);
        when(categoryService.getPublicCategoryTree()).thenReturn(Collections.emptyList());

        // When
        String result = publicMemoController.blog(model, category, pageable, null);

        // Then
        assertEquals("blog", result);
        verify(model).addAttribute("memos", memos);
        verify(model).addAttribute("selectedCategory", category);
    }

    @Test
    void showMemo_LoadsMemoAndRelatedMemos() {
        // Given
        Long memoId = 1L;
        Memo memo = new Memo("/Programming/Java", "This is a test content for Java programming");
        Pageable pageable = PageRequest.of(0, 10);
        Page<Memo> memos = new PageImpl<>(Collections.emptyList());
        
        when(memoService.getPublicMemo(memoId)).thenReturn(memo);
        when(memoService.getPublicMemos(memo.getName(), pageable)).thenReturn(memos);

        // When
        String result = publicMemoController.showMemo(memoId, model, pageable, null);

        // Then
        assertEquals("memo/view", result);
        verify(model).addAttribute("memo", memo);
        verify(model).addAttribute("memos", memos);
        verify(model).addAttribute(eq("pageDescription"), anyString());
    }
    @Test
    void blog_WhenAuthenticated_LoadsAllMemos() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Memo> memos = new PageImpl<>(Collections.emptyList());
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("user", "password", "ROLE_USER");
        when(memoService.getMemos(pageable)).thenReturn(memos);
        when(categoryService.getCategoryTree()).thenReturn(Collections.emptyList());

        // When
        String result = publicMemoController.blog(model, null, pageable, authentication);

        // Then
        assertEquals("blog", result);
        verify(model).addAttribute("memos", memos);
        verify(memoService).getMemos(pageable);
        verify(memoService, never()).getPublicMemos(pageable);
    }

    @Test
    void showMemo_WhenAuthenticated_LoadsPrivateMemo() {
        // Given
        Long memoId = 1L;
        Memo memo = new Memo("/private/Note", "Private content");
        Pageable pageable = PageRequest.of(0, 10);
        Page<Memo> memos = new PageImpl<>(Collections.emptyList());
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("user", "password", "ROLE_USER");
        when(memoService.getMemo(memoId)).thenReturn(memo);
        when(memoService.getMemos(memo.getName(), pageable)).thenReturn(memos);

        // When
        String result = publicMemoController.showMemo(memoId, model, pageable, authentication);

        // Then
        assertEquals("memo/view", result);
        verify(memoService).getMemo(memoId);
        verify(memoService, never()).getPublicMemo(memoId);
    }

}
