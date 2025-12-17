package dev.yunseong.website.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        verify(model).addAttribute("errorMessage", "Page not found");
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
        verify(model).addAttribute("errorMessage", "Internal server error");
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
        verify(model).addAttribute("errorMessage", "Access forbidden");
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
        verify(model).addAttribute("errorMessage", "Bad request");
    }

    @Test
    void handleError_ReturnsDefaultMessageForUnknownStatus() {
        // Given
        when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(502);

        // When
        String viewName = errorController.handleError(request, model);

        // Then
        assertEquals("error", viewName);
        verify(model).addAttribute("statusCode", 502);
        verify(model).addAttribute("errorMessage", "Error occurred (Status: 502)");
    }

    @Test
    void handleError_HandlesNoStatusCode() {
        // Given
        when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(null);

        // When
        String viewName = errorController.handleError(request, model);

        // Then
        assertEquals("error", viewName);
        verify(model).addAttribute("errorMessage", "An unexpected error occurred");
        verify(model, never()).addAttribute(eq("statusCode"), any());
    }

    @Test
    void handleError_IncludesErrorDetailsWhenAvailable() {
        // Given
        when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(500);
        when(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn("Database connection failed");

        // When
        String viewName = errorController.handleError(request, model);

        // Then
        assertEquals("error", viewName);
        verify(model).addAttribute("errorDetails", "Database connection failed");
    }

    @Test
    void handleError_DoesNotIncludeErrorDetailsWhenNull() {
        // Given
        when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(404);
        when(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn(null);

        // When
        String viewName = errorController.handleError(request, model);

        // Then
        assertEquals("error", viewName);
        verify(model, never()).addAttribute(eq("errorDetails"), any());
    }

    @Test
    void handleError_DoesNotIncludeErrorDetailsWhenEmpty() {
        // Given
        when(request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(404);
        when(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn("");

        // When
        String viewName = errorController.handleError(request, model);

        // Then
        assertEquals("error", viewName);
        verify(model, never()).addAttribute(eq("errorDetails"), any());
    }
}
