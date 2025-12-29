package dev.yunseong.website.global.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import dev.yunseong.website.global.controller.ErrorController;

@ExtendWith(MockitoExtension.class)
class ErrorControllerTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private Model model;

    @InjectMocks
    private ErrorController errorController;

    @Test
    void handleError_Returns404Message() {
        // Given
        when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(404);

        // When
        String viewName = errorController.handleError(request, model);

        // Then
        assertEquals("error", viewName);
        verify(model).addAttribute("statusCode", 404);
        verify(model).addAttribute("statusMessage", "Not Found");
    }

    @Test
    void handleError_Returns500Message() {
        // Given
        when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(500);

        // When
        String viewName = errorController.handleError(request, model);

        // Then
        assertEquals("error", viewName);
        verify(model).addAttribute("statusCode", 500);
        verify(model).addAttribute("statusMessage", "Internal Server Error");
    }

    @Test
    void handleError_Returns403Message() {
        // Given
        when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(403);

        // When
        String viewName = errorController.handleError(request, model);

        // Then
        assertEquals("error", viewName);
        verify(model).addAttribute("statusCode", 403);
        verify(model).addAttribute("statusMessage", "Forbidden");
    }

    @Test
    void handleError_Returns400Message() {
        // Given
        when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(400);

        // When
        String viewName = errorController.handleError(request, model);

        // Then
        assertEquals("error", viewName);
        verify(model).addAttribute("statusCode", 400);
        verify(model).addAttribute("statusMessage", "Bad Request");
    }

    @Test
    void handleError_ReturnsDefaultMessageForUnknownStatus() {
        // Given
        when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(418);

        // When
        String viewName = errorController.handleError(request, model);

        // Then
        assertEquals("error", viewName);
        verify(model).addAttribute("statusCode", 418);
        verify(model).addAttribute("statusMessage", "Error");
    }

    @Test
    void handleError_HandlesNoStatusCode() {
        // Given
        when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(null);

        // When
        String viewName = errorController.handleError(request, model);

        // Then
        assertEquals("error", viewName);
        verify(model, never()).addAttribute(eq("statusCode"), any());
        verify(model, never()).addAttribute(eq("statusMessage"), any());
    }
}
