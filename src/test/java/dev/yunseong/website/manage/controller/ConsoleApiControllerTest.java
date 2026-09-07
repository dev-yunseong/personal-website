package dev.yunseong.website.manage.controller;

import dev.yunseong.website.manage.domain.RequestDetail;
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
    void history_ReturnsMappedRequestDetailPage() {
        // Given
        RequestDetail detail = new RequestDetail(
                42L, LocalDateTime.of(2026, 7, 30, 12, 0), "GET", "/public/memos/1", 200, 12,
                "1.1.1.1", "https://referer.com", "Mozilla/5.0", true, 87, "datacenter,ua_mismatch",
                "AU", "Melbourne", -37.814, 144.9633, 20, 13335L, "Cloudflare, Inc.");
        Page<RequestDetail> page = new PageImpl<>(List.of(detail), PageRequest.of(0, 10), 1);
        when(service.getRequestDetailsForLastDays(eq(7), eq(""), any())).thenReturn(page);

        // When
        Map<String, Object> body = controller.history(7, 0, "").getBody();

        // Then
        assertEquals(List.of(detail), body.get("content"));
        assertEquals(1, body.get("totalPages"));
        assertEquals(1L, body.get("totalElements"));
        assertEquals(0, body.get("number"));
        assertEquals(true, body.get("first"));
        assertEquals(true, body.get("last"));

        RequestDetail mapped = ((List<RequestDetail>) body.get("content")).get(0);
        assertEquals(13335L, mapped.asnNumber());
        assertEquals("Cloudflare, Inc.", mapped.asnOrganisation());
    }

    @Test
    void history_WithUnresolvedAsn_LeavesAsnFieldsNull() {
        // Given
        RequestDetail detail = new RequestDetail(
                1L, LocalDateTime.of(2026, 7, 30, 12, 0), "GET", "/public/memos/1", 200, 12,
                "10.0.0.1", null, "Mozilla/5.0", false, null, null,
                null, null, null, null, null, null, null);
        Page<RequestDetail> page = new PageImpl<>(List.of(detail), PageRequest.of(0, 10), 1);
        when(service.getRequestDetailsForLastDays(eq(7), eq(""), any())).thenReturn(page);

        // When
        Map<String, Object> body = controller.history(7, 0, "").getBody();

        // Then
        RequestDetail mapped = ((List<RequestDetail>) body.get("content")).get(0);
        assertNull(mapped.asnNumber());
        assertNull(mapped.asnOrganisation());
    }
}
