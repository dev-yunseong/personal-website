package dev.yunseong.website.global.controller;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.service.MemoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MainControllerTest {

    @Mock
    private MemoService memoService;

    @Mock
    private Model model;

    @InjectMocks
    private MainController mainController;

    @Test
    void index_SetsMetadataAttributes() {
        // Given
        Memo memo = new Memo("/README", "Test content");
        when(memoService.getMemo("/README")).thenReturn(memo);

        // When
        String result = mainController.index(model);

        // Then
        assertEquals("index", result);
        verify(model).addAttribute("memo", memo);
        verify(model).addAttribute("pageTitle", "Yunseong - Personal Website");
        verify(model).addAttribute("pageDescription", "Yunseong의 개인 웹사이트입니다. 개발, 기술 블로그, 그리고 다양한 프로젝트를 공유합니다.");
        verify(model).addAttribute("pageKeywords", "개발, 블로그, Yunseong, 프로그래밍, 기술, 웹개발");
        verify(model).addAttribute("pageUrl", "https://yunseong.dev");
    }

    @Test
    void index_HandlesReadmeMemoNotFound() {
        // Given
        when(memoService.getMemo("/README")).thenThrow(new IllegalArgumentException("Memo not found"));

        // When
        String result = mainController.index(model);

        // Then
        assertEquals("index", result);
        verify(model).addAttribute("memo", null);
        verify(model).addAttribute(eq("pageTitle"), any());
        verify(model).addAttribute(eq("pageDescription"), any());
    }
}
