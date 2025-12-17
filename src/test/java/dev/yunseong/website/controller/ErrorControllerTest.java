package dev.yunseong.website.controller;

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
        verify(model).addAttribute("errorMessage", "Not Found");
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
        verify(model).addAttribute("errorMessage", "Internal Server Error");
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
        verify(model).addAttribute("errorMessage", "Forbidden");
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
        verify(model).addAttribute("errorMessage", "Bad Request");
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
        verify(model).addAttribute("errorMessage", "Error");
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
        verify(model, never()).addAttribute(eq("errorMessage"), any());
    }
}
