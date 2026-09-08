package dev.yunseong.website.manage.controller;

import dev.yunseong.website.manage.domain.RequestDetail;
import dev.yunseong.website.manage.domain.RequestQuery;
import dev.yunseong.website.manage.domain.RequestSummary;
import dev.yunseong.website.manage.service.RequestStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * The two pages that let an aggregate be opened: a filtered request list and
 * one request in full. Both hang off the console's aggregate tabs, which link
 * in with a column value already applied as a filter.
 */
@Controller
@RequestMapping("/admin/console/requests")
@RequiredArgsConstructor
public class RequestBrowseController {

    /** Only a path under the console is accepted as a return destination. */
    private static final String CONSOLE_PREFIX = "/admin/console";
    private static final String CONSOLE_HISTORY = "/admin/console?tab=history";
    private static final int MAX_DAYS = 365;

    private final RequestStatisticsService requestStatisticsService;

    @GetMapping
    public String requests(Model model,
                           @RequestParam(defaultValue = "7") int days,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "") String statusFilter,
                           @RequestParam(defaultValue = "") String uri,
                           @RequestParam(defaultValue = "") String ip,
                           @RequestParam(defaultValue = "") String userAgent,
                           @RequestParam(defaultValue = "") String referer,
                           @RequestParam(defaultValue = "") String countryCode,
                           @RequestParam(defaultValue = "") String from) {
        // Clamped rather than rejected: a page is a destination someone links
        // to, and an error page for days=-1 helps nobody.
        int safeDays = Math.min(Math.max(days, 1), MAX_DAYS);
        int safePage = Math.max(page, 0);

        RequestQuery query = RequestQuery.of(safeDays, statusFilter, uri, ip, userAgent, referer, countryCode);
        Page<RequestSummary> requests = requestStatisticsService.findRequests(query, safePage);

        model.addAttribute("requests", requests);
        model.addAttribute("days", safeDays);
        model.addAttribute("page", safePage);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("uri", uri);
        model.addAttribute("ip", ip);
        model.addAttribute("userAgent", userAgent);
        model.addAttribute("referer", referer);
        model.addAttribute("countryCode", countryCode);
        model.addAttribute("activeFilters", activeFilters(query, safeDays, from));
        model.addAttribute("selfHref", listHref(query, safeDays, safePage, from));
        model.addAttribute("returnHref", returnHref(from));
        return "console/requests";
    }

    @GetMapping("/{id}")
    public String requestDetail(@PathVariable long id,
                                Model model,
                                @RequestParam(defaultValue = "") String from) {
        RequestDetail detail = requestStatisticsService.findRequestDetail(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "request " + id + " was not found"));
        model.addAttribute("detail", detail);
        model.addAttribute("returnHref", returnHref(from));
        return "console/request_detail";
    }

    /**
     * Where "돌아가기" points. {@code from} arrives from the query string, so it
     * is attacker-influenced: anything that is not a path inside the console —
     * an absolute URL, a protocol-relative {@code //host}, another route — is
     * replaced by the console's own history tab rather than followed.
     */
    static String returnHref(String from) {
        return from != null && from.startsWith(CONSOLE_PREFIX) ? from : CONSOLE_HISTORY;
    }

    /**
     * One entry per filter the list is currently narrowed by, each carrying the
     * link that drops just that filter. {@code days} is always set, so it is a
     * period rather than a filter and never appears here.
     */
    private static List<ActiveFilter> activeFilters(RequestQuery query, int days, String from) {
        List<ActiveFilter> filters = new ArrayList<>();
        addFilter(filters, "Status", query.statusFilter(), query.withoutStatusFilter(), days, from);
        addFilter(filters, "URI", query.uri(), query.withoutUri(), days, from);
        addFilter(filters, "IP", query.ip(), query.withoutIp(), days, from);
        addFilter(filters, "User-Agent", query.userAgent(), query.withoutUserAgent(), days, from);
        addFilter(filters, "Referer", query.referer(), query.withoutReferer(), days, from);
        addFilter(filters, "국가", query.countryCode(), query.withoutCountryCode(), days, from);
        return filters;
    }

    private static void addFilter(List<ActiveFilter> filters, String label, String value,
                                  RequestQuery cleared, int days, String from) {
        if (value == null) {
            return;
        }
        // Dropping a filter widens the result, so the old page offset no longer
        // means anything — back to the first page.
        filters.add(new ActiveFilter(label, value, listHref(cleared, days, 0, from)));
    }

    /**
     * The list page's own path and query. Doubles as the {@code from} value the
     * row links carry, so a request detail can return to the exact list that
     * led to it.
     */
    private static String listHref(RequestQuery query, int days, int page, String from) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(CONSOLE_PREFIX + "/requests")
                .queryParam("days", days);
        if (page > 0) {
            builder.queryParam("page", page);
        }
        addParam(builder, "statusFilter", query.statusFilter());
        addParam(builder, "uri", query.uri());
        addParam(builder, "ip", query.ip());
        addParam(builder, "userAgent", query.userAgent());
        addParam(builder, "referer", query.referer());
        addParam(builder, "countryCode", query.countryCode());
        addParam(builder, "from", from == null || from.isBlank() ? null : from);
        return builder.encode().build().toUriString();
    }

    private static void addParam(UriComponentsBuilder builder, String name, String value) {
        if (value != null) {
            builder.queryParam(name, value);
        }
    }

    /** One filter the list header states, with the link that clears it. */
    public record ActiveFilter(String label, String value, String clearHref) {
    }
}
