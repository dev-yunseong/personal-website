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
    void index_LoadsReadmeMemo() {
        // Given
        Memo memo = new Memo("/README", "Test content");
        when(memoService.getMemo("/README")).thenReturn(memo);

        // When
        String result = mainController.index(model);

        // Then
        assertEquals("index", result);
        verify(model).addAttribute("memo", memo);
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
    }
}
