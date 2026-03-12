package dev.yunseong.website.manage.service;

import dev.yunseong.website.manage.domain.RequestStatistics;
import dev.yunseong.website.manage.domain.TimelineStat;
import dev.yunseong.website.manage.domain.UriStat;
import dev.yunseong.website.manage.repository.RequestStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestStatisticsService {
    private static final int PAGE_SIZE = 10;

    private final RequestStatisticsRepository requestStatisticsRepository;
    
    // In-memory storage for request statistics
    private final Queue<RequestStatistics> requestQueue = new ConcurrentLinkedDeque<>();

    public void recordRequest(String uri, String method, String referer, String userAgent, String ipAddress) {
        recordRequest(uri, method, referer, userAgent, ipAddress, null);
    }

    public void recordRequest(String uri, String method, String referer, String userAgent, String ipAddress, Integer statusCode) {
        // Only track requests starting with /public/
        if (uri != null && uri.startsWith("/public/")) {
            RequestStatistics stats = new RequestStatistics(uri, method, referer, userAgent, ipAddress, statusCode);
            requestQueue.add(stats);
            log.debug("Recorded request: {} {} {} (total in memory: {})", method, uri, statusCode, requestQueue.size());
        }
    }

    @Scheduled(fixedRate = 300000) // 5 minutes = 300000 milliseconds
    @Transactional
    public void persistStatistics() {
        if (requestQueue.isEmpty()) {
            log.debug("No statistics to persist");
            return;
        }

        log.info("Persisting {} request statistics to database", requestQueue.size());
        
        // Create a snapshot of current statistics and clear the list
        List<RequestStatistics> snapshot = new ArrayList<>(requestQueue);
        requestQueue.clear();

        // Persist to database
        requestStatisticsRepository.saveAll(snapshot);

        log.info("Successfully persisted {} statistics", snapshot.size());
    }

    @Transactional(readOnly = true)
    public List<RequestStatistics> getStatisticsForLastDays(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        return requestStatisticsRepository.findByCreatedAtAfter(startDate);
    }

    @Transactional(readOnly = true)
    public Page<RequestStatistics> getStatisticsForLastDays(int days, String statusFilter, Pageable pageable) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        if (statusFilter == null || statusFilter.isEmpty()) {
            return requestStatisticsRepository.findByCreatedAtAfter(startDate, pageable);
        }
        int[] range = statusCodeRange(statusFilter);
        return requestStatisticsRepository.findByCreatedAtAfterAndStatusCodeBetween(startDate, range[0], range[1], pageable);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getTopUrisForLastDays(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Object[]> results = requestStatisticsRepository.findTopUrisByRequestCount(startDate);
        
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((Number) row[1]).longValue(),
                        (a, b) -> a
                ));
    }

    @Transactional(readOnly = true)
    public Page<UriStat> getTopUrisPageForLastDays(int days, String topSort, String topStatusFilter, int topPage) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        boolean filtered = topStatusFilter != null && !topStatusFilter.isEmpty();
        Pageable basePageable = PageRequest.of(topPage, PAGE_SIZE);
        if (filtered) {
            int[] range = statusCodeRange(topStatusFilter);
            return switch (topSort != null ? topSort : "count_desc") {
                case "count_asc" -> requestStatisticsRepository.findTopUrisByCountAscAndStatusCodeBetween(
                        startDate, range[0], range[1], basePageable);
                case "key_asc" -> requestStatisticsRepository.findTopUrisSortedByUriAndStatusCodeBetween(
                        startDate, range[0], range[1], PageRequest.of(topPage, PAGE_SIZE, Sort.by("uri").ascending()));
                case "key_desc" -> requestStatisticsRepository.findTopUrisSortedByUriAndStatusCodeBetween(
                        startDate, range[0], range[1], PageRequest.of(topPage, PAGE_SIZE, Sort.by("uri").descending()));
                default -> requestStatisticsRepository.findTopUrisByRequestCountAndStatusCodeBetween(
                        startDate, range[0], range[1], basePageable);
            };
        }
        return switch (topSort != null ? topSort : "count_desc") {
            case "count_asc" -> requestStatisticsRepository.findTopUrisByCountAsc(startDate, basePageable);
            case "key_asc" -> requestStatisticsRepository.findTopUrisSortedByUri(
                    startDate, PageRequest.of(topPage, PAGE_SIZE, Sort.by("uri").ascending()));
            case "key_desc" -> requestStatisticsRepository.findTopUrisSortedByUri(
                    startDate, PageRequest.of(topPage, PAGE_SIZE, Sort.by("uri").descending()));
            default -> requestStatisticsRepository.findTopUrisByRequestCount(startDate, basePageable);
        };
    }

    @Transactional(readOnly = true)
    public Page<UriStat> getTopIpsPageForLastDays(int days, String topSort, String topStatusFilter, int topPage) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        boolean filtered = topStatusFilter != null && !topStatusFilter.isEmpty();
        Pageable basePageable = PageRequest.of(topPage, PAGE_SIZE);
        if (filtered) {
            int[] range = statusCodeRange(topStatusFilter);
            return switch (topSort != null ? topSort : "count_desc") {
                case "count_asc" -> requestStatisticsRepository.findTopIpsByCountAscAndStatusCodeBetween(
                        startDate, range[0], range[1], basePageable);
                case "key_asc" -> requestStatisticsRepository.findTopIpsSortedByKeyAndStatusCodeBetween(
                        startDate, range[0], range[1], PageRequest.of(topPage, PAGE_SIZE, Sort.by("ip").ascending()));
                case "key_desc" -> requestStatisticsRepository.findTopIpsSortedByKeyAndStatusCodeBetween(
                        startDate, range[0], range[1], PageRequest.of(topPage, PAGE_SIZE, Sort.by("ip").descending()));
                default -> requestStatisticsRepository.findTopIpsByRequestCountAndStatusCodeBetween(
                        startDate, range[0], range[1], basePageable);
            };
        }
        return switch (topSort != null ? topSort : "count_desc") {
            case "count_asc" -> requestStatisticsRepository.findTopIpsByCountAsc(startDate, basePageable);
            case "key_asc" -> requestStatisticsRepository.findTopIpsSortedByKey(
                    startDate, PageRequest.of(topPage, PAGE_SIZE, Sort.by("ip").ascending()));
            case "key_desc" -> requestStatisticsRepository.findTopIpsSortedByKey(
                    startDate, PageRequest.of(topPage, PAGE_SIZE, Sort.by("ip").descending()));
            default -> requestStatisticsRepository.findTopIpsByRequestCount(startDate, basePageable);
        };
    }

    @Transactional(readOnly = true)
    public Page<UriStat> getTopUserAgentsPageForLastDays(int days, String topSort, String topStatusFilter, int topPage) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        boolean filtered = topStatusFilter != null && !topStatusFilter.isEmpty();
        Pageable basePageable = PageRequest.of(topPage, PAGE_SIZE);
        if (filtered) {
            int[] range = statusCodeRange(topStatusFilter);
            return switch (topSort != null ? topSort : "count_desc") {
                case "count_asc" -> requestStatisticsRepository.findTopUserAgentsByCountAscAndStatusCodeBetween(
                        startDate, range[0], range[1], basePageable);
                case "key_asc" -> requestStatisticsRepository.findTopUserAgentsSortedByKeyAndStatusCodeBetween(
                        startDate, range[0], range[1], PageRequest.of(topPage, PAGE_SIZE, Sort.by("userAgent").ascending()));
                case "key_desc" -> requestStatisticsRepository.findTopUserAgentsSortedByKeyAndStatusCodeBetween(
                        startDate, range[0], range[1], PageRequest.of(topPage, PAGE_SIZE, Sort.by("userAgent").descending()));
                default -> requestStatisticsRepository.findTopUserAgentsByRequestCountAndStatusCodeBetween(
                        startDate, range[0], range[1], basePageable);
            };
        }
        return switch (topSort != null ? topSort : "count_desc") {
            case "count_asc" -> requestStatisticsRepository.findTopUserAgentsByCountAsc(startDate, basePageable);
            case "key_asc" -> requestStatisticsRepository.findTopUserAgentsSortedByKey(
                    startDate, PageRequest.of(topPage, PAGE_SIZE, Sort.by("userAgent").ascending()));
            case "key_desc" -> requestStatisticsRepository.findTopUserAgentsSortedByKey(
                    startDate, PageRequest.of(topPage, PAGE_SIZE, Sort.by("userAgent").descending()));
            default -> requestStatisticsRepository.findTopUserAgentsByRequestCount(startDate, basePageable);
        };
    }

    @Transactional(readOnly = true)
    public List<TimelineStat> getTimelineForLastDays(int days, String statusFilter) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        if (statusFilter == null || statusFilter.isEmpty()) {
            return requestStatisticsRepository.findDailyRequestCounts(startDate);
        }
        int[] range = statusCodeRange(statusFilter);
        return requestStatisticsRepository.findDailyRequestCountsAndStatusCodeBetween(startDate, range[0], range[1]);
    }

    @Transactional(readOnly = true)
    public long getTotalRequestsForLastDays(int days, String statusFilter) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        if (statusFilter == null || statusFilter.isEmpty()) {
            return requestStatisticsRepository.countByCreatedAtAfter(startDate);
        }
        int[] range = statusCodeRange(statusFilter);
        return requestStatisticsRepository.countByCreatedAtAfterAndStatusCodeBetween(startDate, range[0], range[1]);
    }

    private static int[] statusCodeRange(String statusFilter) {
        return switch (statusFilter) {
            case "2xx" -> new int[]{200, 299};
            case "3xx" -> new int[]{300, 399};
            case "4xx" -> new int[]{400, 499};
            case "5xx" -> new int[]{500, 599};
            default -> new int[]{0, 999};
        };
    }
}
