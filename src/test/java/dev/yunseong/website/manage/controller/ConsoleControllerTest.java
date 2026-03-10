package dev.yunseong.website.manage.controller;

import dev.yunseong.website.manage.domain.RequestStatistics;
import dev.yunseong.website.manage.domain.UriStat;
import dev.yunseong.website.manage.service.RequestStatisticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.ui.Model;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
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
        var historyPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        var topPageable = PageRequest.of(0, 10);
        Page<RequestStatistics> statsPage = new PageImpl<>(statList, historyPageable, 2);
        Page<UriStat> topUrisPage = new PageImpl<>(
                List.of(new UriStat("/public/memos/1", 10L), new UriStat("/public/memos/2", 5L)),
                topPageable, 2);

        when(requestStatisticsService.getStatisticsForLastDays(7, historyPageable)).thenReturn(statsPage);
        when(requestStatisticsService.getTopUrisPageForLastDays(7, topPageable)).thenReturn(topUrisPage);
        when(requestStatisticsService.getTotalRequestsForLastDays(7)).thenReturn(15L);

        // When
        String viewName = consoleController.console(model, 7, 0, 0);

        // Then
        assertEquals("console/dashboard", viewName);
        verify(model).addAttribute("statistics", statsPage);
        verify(model).addAttribute("topUris", topUrisPage);
        verify(model).addAttribute("totalRequests", 15L);
        verify(model).addAttribute("days", 7);
        verify(model).addAttribute("page", 0);
        verify(model).addAttribute("topPage", 0);
    }

    @Test
    void console_WithNoData_ShowsEmptyDashboard() {
        // Given
        var historyPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        var topPageable = PageRequest.of(0, 10);
        Page<RequestStatistics> emptyStats = Page.empty(historyPageable);
        Page<UriStat> emptyTop = Page.empty(topPageable);

        when(requestStatisticsService.getStatisticsForLastDays(7, historyPageable)).thenReturn(emptyStats);
        when(requestStatisticsService.getTopUrisPageForLastDays(7, topPageable)).thenReturn(emptyTop);
        when(requestStatisticsService.getTotalRequestsForLastDays(7)).thenReturn(0L);

        // When
        String viewName = consoleController.console(model, 7, 0, 0);

        // Then
        assertEquals("console/dashboard", viewName);
        verify(model).addAttribute("statistics", emptyStats);
        verify(model).addAttribute("topUris", emptyTop);
        verify(model).addAttribute("totalRequests", 0L);
        verify(model).addAttribute("days", 7);
    }

    @Test
    void console_WithCustomDays_UsesGivenDays() {
        // Given
        var historyPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        var topPageable = PageRequest.of(0, 10);
        Page<RequestStatistics> emptyStats = Page.empty(historyPageable);
        Page<UriStat> emptyTop = Page.empty(topPageable);

        when(requestStatisticsService.getStatisticsForLastDays(1, historyPageable)).thenReturn(emptyStats);
        when(requestStatisticsService.getTopUrisPageForLastDays(1, topPageable)).thenReturn(emptyTop);
        when(requestStatisticsService.getTotalRequestsForLastDays(1)).thenReturn(0L);

        // When
        String viewName = consoleController.console(model, 1, 0, 0);

        // Then
        assertEquals("console/dashboard", viewName);
        verify(requestStatisticsService).getStatisticsForLastDays(eq(1), eq(historyPageable));
        verify(requestStatisticsService).getTopUrisPageForLastDays(eq(1), eq(topPageable));
        verify(requestStatisticsService).getTotalRequestsForLastDays(1);
        verify(model).addAttribute("days", 1);
    }
}
