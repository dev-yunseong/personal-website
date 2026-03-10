package dev.yunseong.website.manage.controller;

import dev.yunseong.website.manage.domain.RequestStatistics;
import dev.yunseong.website.manage.service.RequestStatisticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
        List<RequestStatistics> statList = Arrays.asList(
                new RequestStatistics("/public/memos/1", "GET", "https://google.com", "Mozilla/5.0", "1.1.1.1"),
                new RequestStatistics("/public/memos/2", "GET", null, "Chrome/91.0", "1.1.1.1")
        );
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RequestStatistics> statsPage = new PageImpl<>(statList, pageable, 2);

        Map<String, Long> topUris = new LinkedHashMap<>();
        topUris.put("/public/memos/1", 10L);
        topUris.put("/public/memos/2", 5L);

        when(requestStatisticsService.getStatisticsForLastDays(7, pageable)).thenReturn(statsPage);
        when(requestStatisticsService.getTopUrisForLastDays(7)).thenReturn(topUris);
        when(requestStatisticsService.getTotalRequestsForLastDays(7)).thenReturn(15L);

        // When
        String viewName = consoleController.console(model, 7, pageable);

        // Then
        assertEquals("console/dashboard", viewName);
        verify(model).addAttribute("statistics", statsPage);
        verify(model).addAttribute("topUris", topUris);
        verify(model).addAttribute("totalRequests", 15L);
        verify(model).addAttribute("days", 7);
    }

    @Test
    void console_WithNoData_ShowsEmptyDashboard() {
        // Given
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RequestStatistics> emptyPage = Page.empty(pageable);

        when(requestStatisticsService.getStatisticsForLastDays(7, pageable)).thenReturn(emptyPage);
        when(requestStatisticsService.getTopUrisForLastDays(7)).thenReturn(Collections.emptyMap());
        when(requestStatisticsService.getTotalRequestsForLastDays(7)).thenReturn(0L);

        // When
        String viewName = consoleController.console(model, 7, pageable);

        // Then
        assertEquals("console/dashboard", viewName);
        verify(model).addAttribute("statistics", emptyPage);
        verify(model).addAttribute("topUris", Collections.emptyMap());
        verify(model).addAttribute("totalRequests", 0L);
        verify(model).addAttribute("days", 7);
    }

    @Test
    void console_WithCustomDays_UsesGivenDays() {
        // Given
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RequestStatistics> emptyPage = Page.empty(pageable);

        when(requestStatisticsService.getStatisticsForLastDays(1, pageable)).thenReturn(emptyPage);
        when(requestStatisticsService.getTopUrisForLastDays(1)).thenReturn(Collections.emptyMap());
        when(requestStatisticsService.getTotalRequestsForLastDays(1)).thenReturn(0L);

        // When
        String viewName = consoleController.console(model, 1, pageable);

        // Then
        assertEquals("console/dashboard", viewName);
        verify(requestStatisticsService).getStatisticsForLastDays(1, pageable);
        verify(requestStatisticsService).getTopUrisForLastDays(1);
        verify(requestStatisticsService).getTotalRequestsForLastDays(1);
        verify(model).addAttribute("days", 1);
    }
}
