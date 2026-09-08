package dev.yunseong.website.manage.controller;

import dev.yunseong.website.manage.domain.RequestSummary;
import dev.yunseong.website.manage.service.RequestStatisticsService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsoleApiControllerTest {

    private final RequestStatisticsService service = mock(RequestStatisticsService.class);
    private final ConsoleApiController controller = new ConsoleApiController(service);

    @Test
    void history_ReturnsMappedRequestSummaryPage() {
        // Given
        RequestSummary summary = new RequestSummary(
                42L, LocalDateTime.of(2026, 7, 30, 12, 0), "GET", "/public/memos/1", 200, 12,
                "1.1.1.1", "https://referer.com", "Mozilla/5.0", true, 87, "datacenter,ua_mismatch", "AU");
        Page<RequestSummary> page = new PageImpl<>(List.of(summary), PageRequest.of(0, 10), 1);
        when(service.getRequestSummariesForLastDays(eq(7), eq(""), any())).thenReturn(page);

        // When
        Map<String, Object> body = controller.history(7, 0, "").getBody();

        // Then
        assertEquals(List.of(summary), body.get("content"));
        assertEquals(1, body.get("totalPages"));
        assertEquals(1L, body.get("totalElements"));
        assertEquals(0, body.get("number"));
        assertEquals(true, body.get("first"));
        assertEquals(true, body.get("last"));

        RequestSummary mapped = ((List<RequestSummary>) body.get("content")).get(0);
        assertEquals("/public/memos/1", mapped.uri());
        assertEquals("AU", mapped.countryCode());
        assertEquals("datacenter,ua_mismatch", mapped.botSignals());
    }

    @Test
    void history_WithUnresolvedGeoAndBotScore_LeavesThoseFieldsNull() {
        // Given
        RequestSummary summary = new RequestSummary(
                1L, LocalDateTime.of(2026, 7, 30, 12, 0), "GET", "/public/memos/1", 200, 12,
                "10.0.0.1", null, "Mozilla/5.0", false, null, null, null);
        Page<RequestSummary> page = new PageImpl<>(List.of(summary), PageRequest.of(0, 10), 1);
        when(service.getRequestSummariesForLastDays(eq(7), eq(""), any())).thenReturn(page);

        // When
        Map<String, Object> body = controller.history(7, 0, "").getBody();

        // Then
        RequestSummary mapped = ((List<RequestSummary>) body.get("content")).get(0);
        assertNull(mapped.countryCode());
        assertNull(mapped.botScore());
        assertNull(mapped.referer());
    }
}
