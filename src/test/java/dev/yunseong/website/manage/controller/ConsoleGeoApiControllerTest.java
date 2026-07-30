package dev.yunseong.website.manage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.yunseong.website.manage.domain.GeoRequestStat;
import dev.yunseong.website.manage.service.RequestGeoStatisticsService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsoleGeoApiControllerTest {

    private final RequestGeoStatisticsService service = mock(RequestGeoStatisticsService.class);
    private final ConsoleGeoApiController controller = new ConsoleGeoApiController(service);

    @Test
    void requests_SerializesOnlySafeGeoFields() throws Exception {
        when(service.getRequestsForLastDays(7, "KR", false, 1835848L, 0))
                .thenReturn(new PageImpl<>(List.of(new GeoRequestStat(
                        "/public/memos/1", 200, false, "Seoul", LocalDateTime.of(2026, 7, 30, 12, 0))),
                        PageRequest.of(0, 20), 1));

        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(
                controller.requests("kr", 7, false, 1835848L, 0).getBody());

        assertTrue(json.contains("\"cityName\":\"Seoul\""));
        assertTrue(json.contains("\"uri\":\"/public/memos/1\""));
        assertFalse(json.contains("\"ip\""));
        assertFalse(json.contains("\"userAgent\""));
        assertFalse(json.contains("\"referer\""));
    }

    @Test
    void endpoints_RejectInvalidBoundsAndCountryCodes() {
        assertThrows(ResponseStatusException.class, () -> controller.countries(0, 0, false));
        assertThrows(ResponseStatusException.class, () -> controller.countries(7, -1, false));
        assertThrows(ResponseStatusException.class, () -> controller.cities("KOR", 7, false));
        assertThrows(ResponseStatusException.class, () -> controller.requests("KR", 91, false, null, 0));
        assertThrows(ResponseStatusException.class, () -> controller.requests("KR", 7, false, 0L, 0));
    }
}
