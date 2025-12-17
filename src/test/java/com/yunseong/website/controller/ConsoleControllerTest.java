package com.yunseong.website.controller;

import com.yunseong.website.domain.RequestStatistics;
import com.yunseong.website.service.RequestStatisticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsoleControllerTest {

    @Mock
    private RequestStatisticsService requestStatisticsService;

    @Mock
    private Model model;

    @InjectMocks
    private ConsoleController consoleController;

    @Test
    void console_ReturnsConsoleView() {
        // Given
        List<RequestStatistics> stats = Arrays.asList(
                new RequestStatistics("/public/memos/1", "GET", 5L),
                new RequestStatistics("/public/memos/2", "GET", 3L)
        );
        Map<String, Long> topUris = new LinkedHashMap<>();
        topUris.put("/public/memos/1", 10L);
        topUris.put("/public/memos/2", 5L);

        when(requestStatisticsService.getStatisticsForLastDays(7)).thenReturn(stats);
        when(requestStatisticsService.getTopUrisForLastDays(7)).thenReturn(topUris);
        when(requestStatisticsService.getTotalRequestsForLastDays(7)).thenReturn(15L);

        // When
        String viewName = consoleController.console(model);

        // Then
        assertEquals("console/dashboard", viewName);
        verify(model).addAttribute("statistics", stats);
        verify(model).addAttribute("topUris", topUris);
        verify(model).addAttribute("totalRequests", 15L);
    }

    @Test
    void console_WithNoData_ShowsEmptyDashboard() {
        // Given
        when(requestStatisticsService.getStatisticsForLastDays(7)).thenReturn(Collections.emptyList());
        when(requestStatisticsService.getTopUrisForLastDays(7)).thenReturn(Collections.emptyMap());
        when(requestStatisticsService.getTotalRequestsForLastDays(7)).thenReturn(0L);

        // When
        String viewName = consoleController.console(model);

        // Then
        assertEquals("console/dashboard", viewName);
        verify(model).addAttribute("statistics", Collections.emptyList());
        verify(model).addAttribute("topUris", Collections.emptyMap());
        verify(model).addAttribute("totalRequests", 0L);
    }
}
