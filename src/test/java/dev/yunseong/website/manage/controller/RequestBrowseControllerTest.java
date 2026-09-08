package dev.yunseong.website.manage.controller;

import dev.yunseong.website.manage.domain.RequestDetail;
import dev.yunseong.website.manage.domain.RequestQuery;
import dev.yunseong.website.manage.domain.RequestSummary;
import dev.yunseong.website.manage.service.RequestStatisticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestBrowseControllerTest {

    private static final String CONSOLE_HISTORY = "/admin/console?tab=history";

    private final RequestStatisticsService service = mock(RequestStatisticsService.class);
    private final RequestBrowseController controller = new RequestBrowseController(service);

    private static RequestSummary summary() {
        return new RequestSummary(42L, LocalDateTime.of(2026, 7, 30, 12, 0), "GET", "/public/memos", 200, 12,
                "1.1.1.1", null, "curl/8.0", false, null, null, "KR");
    }

    private static RequestDetail detail() {
        return new RequestDetail(42L, LocalDateTime.of(2026, 7, 30, 12, 0), "GET", "/public/memos", 200, 12,
                "1.1.1.1", null, "curl/8.0", false, null, null, "KR", "Seoul", 37.56, 126.98, 20,
                15169L, "Google LLC");
    }

    private Model listPage(String from) {
        when(service.findRequests(any(RequestQuery.class), anyInt()))
                .thenReturn(new PageImpl<>(List.of(summary()), PageRequest.of(0, 10), 1));
        Model model = new ExtendedModelMap();
        controller.requests(model, 7, 0, "", "", "", "", "", "", from);
        return model;
    }

    @Test
    void requests_RendersTheListViewWithThePageOfSummaries() {
        when(service.findRequests(any(RequestQuery.class), anyInt()))
                .thenReturn(new PageImpl<>(List.of(summary()), PageRequest.of(0, 10), 1));
        Model model = new ExtendedModelMap();

        String view = controller.requests(model, 7, 0, "", "", "", "", "", "", "");

        assertEquals("console/requests", view);
        assertEquals(1, ((Page<?>) model.getAttribute("requests")).getTotalElements());
    }

    @Test
    void requests_NormalisesBlankFiltersToNullBeforeTheService() {
        ArgumentCaptor<RequestQuery> query = ArgumentCaptor.forClass(RequestQuery.class);
        when(service.findRequests(query.capture(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        controller.requests(new ExtendedModelMap(), 7, 0, "", "/public/memos", "", "", "", "", "");

        assertEquals("/public/memos", query.getValue().uri());
        assertEquals(null, query.getValue().ip());
        assertEquals(null, query.getValue().statusFilter());
    }

    @Test
    void requests_StatesEachActiveFilterWithALinkThatClearsIt() {
        when(service.findRequests(any(RequestQuery.class), anyInt()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        Model model = new ExtendedModelMap();

        controller.requests(model, 7, 0, "4xx", "/public/memos", "", "", "", "KR", "");

        List<RequestBrowseController.ActiveFilter> filters =
                (List<RequestBrowseController.ActiveFilter>) model.getAttribute("activeFilters");
        assertEquals(List.of("Status", "URI", "국가"), filters.stream().map(f -> f.label()).toList());

        RequestBrowseController.ActiveFilter uriFilter = filters.get(1);
        assertEquals("/public/memos", uriFilter.value());
        // The link keeps the other filters and drops only this one.
        assertFalse(uriFilter.clearHref().contains("uri="));
        assertTrue(uriFilter.clearHref().contains("statusFilter=4xx"));
        assertTrue(uriFilter.clearHref().contains("countryCode=KR"));
    }

    @Test
    void requests_ClampsNegativeDaysAndPageInsteadOfFailing() {
        ArgumentCaptor<Integer> page = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<RequestQuery> query = ArgumentCaptor.forClass(RequestQuery.class);
        when(service.findRequests(query.capture(), page.capture()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        controller.requests(new ExtendedModelMap(), -5, -1, "", "", "", "", "", "", "");

        assertEquals(1, query.getValue().days());
        assertEquals(0, page.getValue());
    }

    @Test
    void requests_SelfHrefCarriesTheActiveFiltersSoADetailPageCanReturnToThisList() {
        when(service.findRequests(any(RequestQuery.class), anyInt()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));
        Model model = new ExtendedModelMap();

        controller.requests(model, 30, 2, "", "/public/memos", "", "Mozilla/5.0 (X11; Linux)", "", "", "");

        String selfHref = (String) model.getAttribute("selfHref");
        assertTrue(selfHref.startsWith("/admin/console/requests?"));
        assertTrue(selfHref.contains("days=30"));
        assertTrue(selfHref.contains("page=2"));
        assertTrue(selfHref.contains("uri=/public/memos"));
        // Anything not legal in a query value is percent-encoded, so the whole
        // href survives being carried back as one `from` parameter.
        assertTrue(selfHref.contains("userAgent=Mozilla/5.0%20(X11;%20Linux)"), selfHref);
    }

    @Test
    void requestDetail_RendersTheDetailViewForAStoredRequest() {
        when(service.findRequestDetail(42L)).thenReturn(Optional.of(detail()));
        Model model = new ExtendedModelMap();

        String view = controller.requestDetail(42L, model, "");

        assertEquals("console/request_detail", view);
        assertEquals(detail(), model.getAttribute("detail"));
    }

    @Test
    void requestDetail_WithUnknownId_Returns404() {
        when(service.findRequestDetail(999L)).thenReturn(Optional.empty());

        ResponseStatusException thrown = assertThrows(ResponseStatusException.class,
                () -> controller.requestDetail(999L, new ExtendedModelMap(), ""));

        assertEquals(HttpStatus.NOT_FOUND, thrown.getStatusCode());
    }

    /**
     * The one input that becomes a link href. Anything that could leave the
     * console — another origin, a protocol-relative host, a scheme, another
     * route on this site — is replaced rather than followed.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "https://evil.example/steal",
            "//evil.example/steal",
            "http://localhost:8080/admin/console",
            "javascript:alert(1)",
            "/admin/chat",
            "/public/memos",
            "",
            "   "
    })
    void from_ThatIsNotAConsolePath_FallsBackToTheConsoleHistoryTab(String from) {
        assertEquals(CONSOLE_HISTORY, listPage(from).getAttribute("returnHref"));

        when(service.findRequestDetail(42L)).thenReturn(Optional.of(detail()));
        Model detailModel = new ExtendedModelMap();
        controller.requestDetail(42L, detailModel, from);
        assertEquals(CONSOLE_HISTORY, detailModel.getAttribute("returnHref"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/admin/console",
            "/admin/console?tab=top",
            "/admin/console/requests?days=7&uri=%2Fpublic%2Fmemos"
    })
    void from_ThatIsAConsolePath_IsKept(String from) {
        assertEquals(from, listPage(from).getAttribute("returnHref"));
    }

    @Test
    void from_IsNeverNull_SoTheReturnLinkAlwaysHasADestination() {
        assertEquals(CONSOLE_HISTORY, RequestBrowseController.returnHref(null));
    }
}
