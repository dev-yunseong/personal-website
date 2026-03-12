package dev.yunseong.website.manage.controller;

import dev.yunseong.website.manage.domain.RequestStatistics;
import dev.yunseong.website.manage.domain.TimelineStat;
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

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
        Page<RequestStatistics> statsPage = new PageImpl<>(statList, historyPageable, 2);
        Page<UriStat> topUrisPage = new PageImpl<>(
                List.of(new UriStat("/public/memos/1", 10L), new UriStat("/public/memos/2", 5L)),
                PageRequest.of(0, 10), 2);

        when(requestStatisticsService.getStatisticsForLastDays(7, "", historyPageable)).thenReturn(statsPage);
        when(requestStatisticsService.getTopUrisPageForLastDays(7, "count_desc", "", 0)).thenReturn(topUrisPage);
        when(requestStatisticsService.getTotalRequestsForLastDays(7, "")).thenReturn(15L);
        when(requestStatisticsService.getTimelineForLastDays(7, "")).thenReturn(List.of());

        // When
        String viewName = consoleController.console(model, 7, 0, 0, "", "", "count_desc", "top", "uri", "");

        // Then
        assertEquals("console/dashboard", viewName);
        verify(model).addAttribute("statistics", statsPage);
        verify(model).addAttribute("topUris", topUrisPage);
        verify(model).addAttribute("totalRequests", 15L);
        verify(model).addAttribute("days", 7);
        verify(model).addAttribute("page", 0);
        verify(model).addAttribute("topPage", 0);
        verify(model).addAttribute("statusFilter", "");
        verify(model).addAttribute("topStatusFilter", "");
        verify(model).addAttribute("topSort", "count_desc");
        verify(model).addAttribute("tab", "top");
        verify(model).addAttribute("topGroupBy", "uri");
        verify(model).addAttribute("timelineStatusFilter", "");
        verify(model).addAttribute("topSortLabel", "Requests ↓");
    }

    @Test
    void console_WithNoData_ShowsEmptyDashboard() {
        // Given
        var historyPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RequestStatistics> emptyStats = Page.empty(historyPageable);
        Page<UriStat> emptyTop = Page.empty(PageRequest.of(0, 10));

        when(requestStatisticsService.getStatisticsForLastDays(7, "", historyPageable)).thenReturn(emptyStats);
        when(requestStatisticsService.getTopUrisPageForLastDays(7, "count_desc", "", 0)).thenReturn(emptyTop);
        when(requestStatisticsService.getTotalRequestsForLastDays(7, "")).thenReturn(0L);
        when(requestStatisticsService.getTimelineForLastDays(7, "")).thenReturn(List.of());

        // When
        String viewName = consoleController.console(model, 7, 0, 0, "", "", "count_desc", "top", "uri", "");

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
        Page<RequestStatistics> emptyStats = Page.empty(historyPageable);
        Page<UriStat> emptyTop = Page.empty(PageRequest.of(0, 10));

        when(requestStatisticsService.getStatisticsForLastDays(1, "", historyPageable)).thenReturn(emptyStats);
        when(requestStatisticsService.getTopUrisPageForLastDays(1, "count_desc", "", 0)).thenReturn(emptyTop);
        when(requestStatisticsService.getTotalRequestsForLastDays(1, "")).thenReturn(0L);
        when(requestStatisticsService.getTimelineForLastDays(1, "")).thenReturn(List.of());

        // When
        String viewName = consoleController.console(model, 1, 0, 0, "", "", "count_desc", "top", "uri", "");

        // Then
        assertEquals("console/dashboard", viewName);
        verify(requestStatisticsService).getStatisticsForLastDays(eq(1), eq(""), eq(historyPageable));
        verify(requestStatisticsService).getTopUrisPageForLastDays(eq(1), eq("count_desc"), eq(""), eq(0));
        verify(requestStatisticsService).getTotalRequestsForLastDays(eq(1), eq(""));
        verify(model).addAttribute("days", 1);
    }

    @Test
    void console_WithStatusFilter_FiltersHistory() {
        // Given
        var historyPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RequestStatistics> emptyStats = Page.empty(historyPageable);
        Page<UriStat> emptyTop = Page.empty(PageRequest.of(0, 10));

        when(requestStatisticsService.getStatisticsForLastDays(7, "2xx", historyPageable)).thenReturn(emptyStats);
        when(requestStatisticsService.getTopUrisPageForLastDays(7, "count_desc", "", 0)).thenReturn(emptyTop);
        when(requestStatisticsService.getTotalRequestsForLastDays(7, "2xx")).thenReturn(0L);
        when(requestStatisticsService.getTimelineForLastDays(7, "")).thenReturn(List.of());

        // When
        String viewName = consoleController.console(model, 7, 0, 0, "2xx", "", "count_desc", "history", "uri", "");

        // Then
        assertEquals("console/dashboard", viewName);
        verify(requestStatisticsService).getStatisticsForLastDays(eq(7), eq("2xx"), eq(historyPageable));
        verify(requestStatisticsService).getTotalRequestsForLastDays(eq(7), eq("2xx"));
        verify(model).addAttribute("statusFilter", "2xx");
    }

    @Test
    void console_WithTopSort_PassesSortToService() {
        // Given
        var historyPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RequestStatistics> emptyStats = Page.empty(historyPageable);
        Page<UriStat> emptyTop = Page.empty(PageRequest.of(0, 10));

        when(requestStatisticsService.getStatisticsForLastDays(7, "", historyPageable)).thenReturn(emptyStats);
        when(requestStatisticsService.getTopUrisPageForLastDays(7, "key_asc", "", 0)).thenReturn(emptyTop);
        when(requestStatisticsService.getTotalRequestsForLastDays(7, "")).thenReturn(0L);
        when(requestStatisticsService.getTimelineForLastDays(7, "")).thenReturn(List.of());

        // When
        String viewName = consoleController.console(model, 7, 0, 0, "", "", "key_asc", "top", "uri", "");

        // Then
        assertEquals("console/dashboard", viewName);
        verify(requestStatisticsService).getTopUrisPageForLastDays(eq(7), eq("key_asc"), eq(""), eq(0));
        verify(model).addAttribute("topSort", "key_asc");
        verify(model).addAttribute("topSortLabel", "Key A→Z");
    }

    @Test
    void console_WithTopStatusFilter_FiltersTopPages() {
        // Given
        var historyPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RequestStatistics> emptyStats = Page.empty(historyPageable);
        Page<UriStat> emptyTop = Page.empty(PageRequest.of(0, 10));

        when(requestStatisticsService.getStatisticsForLastDays(7, "", historyPageable)).thenReturn(emptyStats);
        when(requestStatisticsService.getTopUrisPageForLastDays(7, "count_desc", "4xx", 0)).thenReturn(emptyTop);
        when(requestStatisticsService.getTotalRequestsForLastDays(7, "")).thenReturn(0L);
        when(requestStatisticsService.getTimelineForLastDays(7, "")).thenReturn(List.of());

        // When
        String viewName = consoleController.console(model, 7, 0, 0, "", "4xx", "count_desc", "top", "uri", "");

        // Then
        assertEquals("console/dashboard", viewName);
        verify(requestStatisticsService).getTopUrisPageForLastDays(eq(7), eq("count_desc"), eq("4xx"), eq(0));
        verify(model).addAttribute("topStatusFilter", "4xx");
    }

    @Test
    void console_WithGroupByIp_CallsIpService() {
        // Given
        var historyPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RequestStatistics> emptyStats = Page.empty(historyPageable);
        Page<UriStat> emptyTop = Page.empty(PageRequest.of(0, 10));

        when(requestStatisticsService.getStatisticsForLastDays(7, "", historyPageable)).thenReturn(emptyStats);
        when(requestStatisticsService.getTopIpsPageForLastDays(7, "count_desc", "", 0)).thenReturn(emptyTop);
        when(requestStatisticsService.getTotalRequestsForLastDays(7, "")).thenReturn(0L);
        when(requestStatisticsService.getTimelineForLastDays(7, "")).thenReturn(List.of());

        // When
        String viewName = consoleController.console(model, 7, 0, 0, "", "", "count_desc", "top", "ip", "");

        // Then
        assertEquals("console/dashboard", viewName);
        verify(requestStatisticsService).getTopIpsPageForLastDays(eq(7), eq("count_desc"), eq(""), eq(0));
        verify(requestStatisticsService, never()).getTopUrisPageForLastDays(any(), any(), any(), anyInt());
        verify(model).addAttribute("topGroupBy", "ip");
        verify(model).addAttribute("topGroupByLabel", "IP Address");
    }

    @Test
    void console_WithGroupByUa_CallsUserAgentService() {
        // Given
        var historyPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RequestStatistics> emptyStats = Page.empty(historyPageable);
        Page<UriStat> emptyTop = Page.empty(PageRequest.of(0, 10));

        when(requestStatisticsService.getStatisticsForLastDays(7, "", historyPageable)).thenReturn(emptyStats);
        when(requestStatisticsService.getTopUserAgentsPageForLastDays(7, "count_desc", "", 0)).thenReturn(emptyTop);
        when(requestStatisticsService.getTotalRequestsForLastDays(7, "")).thenReturn(0L);
        when(requestStatisticsService.getTimelineForLastDays(7, "")).thenReturn(List.of());

        // When
        String viewName = consoleController.console(model, 7, 0, 0, "", "", "count_desc", "top", "ua", "");

        // Then
        assertEquals("console/dashboard", viewName);
        verify(requestStatisticsService).getTopUserAgentsPageForLastDays(eq(7), eq("count_desc"), eq(""), eq(0));
        verify(requestStatisticsService, never()).getTopUrisPageForLastDays(any(), any(), any(), anyInt());
        verify(model).addAttribute("topGroupBy", "ua");
        verify(model).addAttribute("topGroupByLabel", "User Agent");
    }

    @Test
    void console_WithTimelineTab_PassesTimelineData() {
        // Given
        var historyPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RequestStatistics> emptyStats = Page.empty(historyPageable);
        Page<UriStat> emptyTop = Page.empty(PageRequest.of(0, 10));
        List<TimelineStat> timelineData = List.of(
                new TimelineStat(LocalDate.of(2024, 1, 1), 10L),
                new TimelineStat(LocalDate.of(2024, 1, 2), 20L)
        );

        when(requestStatisticsService.getStatisticsForLastDays(7, "", historyPageable)).thenReturn(emptyStats);
        when(requestStatisticsService.getTopUrisPageForLastDays(7, "count_desc", "", 0)).thenReturn(emptyTop);
        when(requestStatisticsService.getTotalRequestsForLastDays(7, "")).thenReturn(30L);
        when(requestStatisticsService.getTimelineForLastDays(7, "")).thenReturn(timelineData);

        // When
        String viewName = consoleController.console(model, 7, 0, 0, "", "", "count_desc", "timeline", "uri", "");

        // Then
        assertEquals("console/dashboard", viewName);
        verify(requestStatisticsService).getTimelineForLastDays(eq(7), eq(""));
        verify(model).addAttribute("timeline", timelineData);
        verify(model).addAttribute("tab", "timeline");
    }

    @Test
    void console_WithTimelineStatusFilter_FiltersTimeline() {
        // Given
        var historyPageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RequestStatistics> emptyStats = Page.empty(historyPageable);
        Page<UriStat> emptyTop = Page.empty(PageRequest.of(0, 10));
        List<TimelineStat> timelineData = List.of(new TimelineStat(LocalDate.of(2024, 1, 1), 5L));

        when(requestStatisticsService.getStatisticsForLastDays(7, "", historyPageable)).thenReturn(emptyStats);
        when(requestStatisticsService.getTopUrisPageForLastDays(7, "count_desc", "", 0)).thenReturn(emptyTop);
        when(requestStatisticsService.getTotalRequestsForLastDays(7, "")).thenReturn(5L);
        when(requestStatisticsService.getTimelineForLastDays(7, "2xx")).thenReturn(timelineData);

        // When
        String viewName = consoleController.console(model, 7, 0, 0, "", "", "count_desc", "timeline", "uri", "2xx");

        // Then
        assertEquals("console/dashboard", viewName);
        verify(requestStatisticsService).getTimelineForLastDays(eq(7), eq("2xx"));
        verify(model).addAttribute("timelineStatusFilter", "2xx");
        verify(model).addAttribute("timeline", timelineData);
    }
}
