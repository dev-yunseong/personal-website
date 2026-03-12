package dev.yunseong.website.manage.controller;

import dev.yunseong.website.manage.domain.RequestStatistics;
import dev.yunseong.website.manage.domain.TimelineStat;
import dev.yunseong.website.manage.domain.UriStat;
import dev.yunseong.website.manage.service.RequestStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/console")
@RequiredArgsConstructor
public class ConsoleApiController {

    private static final int PAGE_SIZE = 10;

    private final RequestStatisticsService requestStatisticsService;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "") String statusFilter) {
        long totalRequests = requestStatisticsService.getTotalRequestsForLastDays(days, statusFilter);
        Page<UriStat> topUris = requestStatisticsService.getTopUrisPageForLastDays(days, "count_desc", "", 0);
        Page<RequestStatistics> history = requestStatisticsService.getStatisticsForLastDays(
                days, statusFilter, PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(Map.of(
                "totalRequests", totalRequests,
                "uniqueUris", topUris.getTotalElements(),
                "dataPoints", history.getTotalElements()
        ));
    }

    @GetMapping("/top")
    public ResponseEntity<Map<String, Object>> top(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "0") int topPage,
            @RequestParam(defaultValue = "count_desc") String topSort,
            @RequestParam(defaultValue = "") String topStatusFilter,
            @RequestParam(defaultValue = "uri") String topGroupBy) {
        Page<UriStat> page = switch (topGroupBy) {
            case "ip" -> requestStatisticsService.getTopIpsPageForLastDays(days, topSort, topStatusFilter, topPage);
            case "ua" -> requestStatisticsService.getTopUserAgentsPageForLastDays(days, topSort, topStatusFilter, topPage);
            default  -> requestStatisticsService.getTopUrisPageForLastDays(days, topSort, topStatusFilter, topPage);
        };
        String groupByLabel = switch (topGroupBy) {
            case "ip" -> "IP Address";
            case "ua" -> "User Agent";
            default   -> "URI";
        };
        return ResponseEntity.ok(Map.of(
                "content", page.getContent(),
                "totalPages", page.getTotalPages(),
                "totalElements", page.getTotalElements(),
                "number", page.getNumber(),
                "first", page.isFirst(),
                "last", page.isLast(),
                "groupByLabel", groupByLabel
        ));
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> history(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "") String statusFilter) {
        Page<RequestStatistics> result = requestStatisticsService.getStatisticsForLastDays(
                days, statusFilter,
                PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(Map.of(
                "content", result.getContent(),
                "totalPages", result.getTotalPages(),
                "totalElements", result.getTotalElements(),
                "number", result.getNumber(),
                "first", result.isFirst(),
                "last", result.isLast()
        ));
    }

    @GetMapping("/timeline")
    public ResponseEntity<List<TimelineStat>> timeline(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "") String statusFilter,
            @RequestParam(defaultValue = "day") String resolution) {
        List<TimelineStat> data = requestStatisticsService.getTimelineForLastDays(days, statusFilter, resolution);
        return ResponseEntity.ok(data);
    }
}
