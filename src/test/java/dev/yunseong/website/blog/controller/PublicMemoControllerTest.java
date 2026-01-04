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
import org.springframework.ui.Model;

import java.util.Collections;
import java.util.List;

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
    void blog_SetsMetadataForAllCategories() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Memo> memos = new PageImpl<>(Collections.emptyList());
        when(memoService.getMemos(pageable)).thenReturn(memos);
        when(categoryService.getCategoryTree()).thenReturn(Collections.emptyList());

        // When
        String result = publicMemoController.blog(model, null, pageable);

        // Then
        assertEquals("blog", result);
        verify(model).addAttribute("pageTitle", "Blog | Yunseong");
        verify(model).addAttribute(eq("pageDescription"), contains("블로그"));
        verify(model).addAttribute(eq("pageUrl"), contains("/public/memos"));
    }

    @Test
    void blog_SetsMetadataForSpecificCategory() {
        // Given
        String category = "Java";
        Pageable pageable = PageRequest.of(0, 10);
        Page<Memo> memos = new PageImpl<>(Collections.emptyList());
        when(memoService.getMemos(category, pageable)).thenReturn(memos);
        when(categoryService.getCategoryTree()).thenReturn(Collections.emptyList());

        // When
        String result = publicMemoController.blog(model, category, pageable);

        // Then
        assertEquals("blog", result);
        verify(model).addAttribute("pageTitle", "Blog - Java | Yunseong");
        verify(model).addAttribute(eq("pageDescription"), contains("Java"));
        verify(model).addAttribute(eq("pageUrl"), contains("category=" + category));
    }

    @Test
    void showMemo_SetsMetadataForMemo() {
        // Given
        Long memoId = 1L;
        Memo memo = new Memo("/Programming/Java", "This is a test content for Java programming");
        Pageable pageable = PageRequest.of(0, 10);
        Page<Memo> memos = new PageImpl<>(Collections.emptyList());
        
        when(memoService.getMemo(memoId)).thenReturn(memo);
        when(memoService.getMemos(memo.getName(), pageable)).thenReturn(memos);

        // When
        String result = publicMemoController.showMemo(memoId, model, pageable);

        // Then
        assertEquals("memo/view", result);
        verify(model).addAttribute("memo", memo);
        verify(model).addAttribute("pageTitle", "Java | Yunseong");
        verify(model).addAttribute(eq("pageDescription"), anyString());
        verify(model).addAttribute(eq("pageUrl"), contains("/public/memos/" + memoId));
    }
}
