package dev.yunseong.website.manage.service;

import dev.yunseong.website.manage.domain.AutonomousSystem;
import dev.yunseong.website.manage.domain.BotDetector;
import dev.yunseong.website.manage.domain.BotVerdict;
import dev.yunseong.website.manage.domain.GeoLocation;
import dev.yunseong.website.manage.domain.RequestDetail;
import dev.yunseong.website.manage.domain.RequestFingerprint;
import dev.yunseong.website.manage.domain.RequestQuery;
import dev.yunseong.website.manage.domain.RequestStatistics;
import dev.yunseong.website.manage.domain.RequestSummary;
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

import java.sql.Date;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestStatisticsService {
    private static final int PAGE_SIZE = 10;

    private final RequestStatisticsRepository requestStatisticsRepository;
    private final GeoIpLocationResolver geoIpLocationResolver;
    private final AsnResolver asnResolver;

    // In-memory storage for request statistics
    private final Queue<RequestStatistics> requestQueue = new ConcurrentLinkedDeque<>();

    public void recordRequest(String uri, String method, String referer, RequestFingerprint client, String ipAddress,
                              Integer statusCode, Integer durationMs) {
        if (!isCollectedUri(uri)) {
            return;
        }
        // Both resolved here, not on read: local mmdb lookups, so reads stay a plain
        // GROUP BY. The network is evidence for the verdict, so it is looked up first.
        AutonomousSystem network = asnResolver.resolve(ipAddress);
        BotVerdict verdict = BotDetector.classify(client, network);
        GeoLocation location = geoIpLocationResolver.resolve(ipAddress);
        requestQueue.add(new RequestStatistics(
                uri, method, referer, client.userAgent(), ipAddress, statusCode, verdict, durationMs, location));
        log.debug("Recorded request: {} {} {} bot={} score={} signals={} {}ms country={} city={} (total in memory: {})",
                method, uri, statusCode, verdict.bot(), verdict.score(), verdict.signals(), durationMs,
                location == null ? null : location.countryCode(),
                location == null ? null : location.cityName(),
                requestQueue.size());
    }

    /**
     * Whitelist of the public request surface. Static assets, admin routes, and
     * infrastructure endpoints fall outside it by construction, so no deny list
     * is needed.
     */
    private static boolean isCollectedUri(String uri) {
        if (uri == null) {
            return false;
        }
        return uri.equals("/") || uri.startsWith("/public/") || uri.startsWith("/api/public/");
    }

    @Scheduled(fixedRate = 300000) // 5 minutes = 300000 milliseconds
    @Transactional
    public void persistStatistics() {
        if (requestQueue.isEmpty()) {
            log.debug("No statistics to persist");
            return;
        }

        log.info("Persisting {} request statistics to database", requestQueue.size());

        // Drain with poll(): copy-then-clear is not atomic, so anything recorded
        // between the copy and the clear was deleted without ever being saved.
        // poll() takes and removes the head in one operation, so that window is gone.
        // FIFO is preserved: add() appends at the tail, poll() takes the head.
        List<RequestStatistics> snapshot = new ArrayList<>();
        RequestStatistics item;
        while ((item = requestQueue.poll()) != null) {
            snapshot.add(item);
        }

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

    /**
     * The history table's read model. No ASN: that is one mmdb lookup per row
     * and nothing in the list renders it, so it is resolved once on the detail
     * page instead — see {@link #findRequestDetail(long)}.
     */
    @Transactional(readOnly = true)
    public Page<RequestSummary> getRequestSummariesForLastDays(int days, String statusFilter, Pageable pageable) {
        return getStatisticsForLastDays(days, statusFilter, pageable).map(RequestSummary::of);
    }

    /**
     * The request list behind {@code /admin/console/requests}. Blank filters
     * were already normalised to null by {@link RequestQuery#of}; a null status
     * range drops the status clause entirely rather than widening it, so rows
     * with no recorded status code stay in the unfiltered result.
     */
    @Transactional(readOnly = true)
    public Page<RequestSummary> findRequests(RequestQuery query, int page) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(query.days());
        int[] range = query.statusFilter() == null ? null : statusCodeRange(query.statusFilter());
        return requestStatisticsRepository.findMatchingRequests(
                        startDate,
                        range == null ? null : range[0],
                        range == null ? null : range[1],
                        query.uri(),
                        query.ip(),
                        query.userAgent(),
                        query.referer(),
                        query.countryCode(),
                        PageRequest.of(page, PAGE_SIZE))
                .map(RequestSummary::of);
    }

    /**
     * One request with every stored field plus its autonomous system, resolved
     * from the stored IP at read time because it is not a stored column. A
     * missing database or a private range yields a null ASN rather than an
     * error, so the page always renders.
     */
    @Transactional(readOnly = true)
    public Optional<RequestDetail> findRequestDetail(long id) {
        return requestStatisticsRepository.findById(id)
                .map(row -> RequestDetail.of(row, asnResolver.resolve(row.getIp())));
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
        return getTimelineForLastDays(days, statusFilter, "day");
    }

    @Transactional(readOnly = true)
    public List<TimelineStat> getTimelineForLastDays(int days, String statusFilter, String resolution) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        boolean filtered = statusFilter != null && !statusFilter.isEmpty();
        int[] range = filtered ? statusCodeRange(statusFilter) : null;

        return switch (resolution) {
            case "hour" -> {
                List<Object[]> rows = filtered
                        ? requestStatisticsRepository.findHourlyRequestCountsAndStatusCodeBetween(startDate, range[0], range[1])
                        : requestStatisticsRepository.findHourlyRequestCounts(startDate);
                yield rows.stream()
                        .map(r -> {
                            LocalDateTime dt = (LocalDateTime) r[0];
                            String label = dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                            return new TimelineStat(label, ((Number) r[1]).longValue());
                        })
                        .collect(Collectors.toList());
            }
            case "week" -> {
                // Fetch daily data and aggregate in Java by ISO week start (Monday)
                List<Object[]> rows = filtered
                        ? requestStatisticsRepository.findDailyRequestCountsAndStatusCodeBetween(startDate, range[0], range[1])
                        : requestStatisticsRepository.findDailyRequestCounts(startDate);
                TreeMap<String, Long> weekly = new TreeMap<>();
                for (Object[] r : rows) {
                    LocalDate date = ((Date) r[0]).toLocalDate();
                    LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    String label = weekStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    weekly.merge(label, ((Number) r[1]).longValue(), Long::sum);
                }
                yield weekly.entrySet().stream()
                        .map(e -> new TimelineStat(e.getKey(), e.getValue()))
                        .collect(Collectors.toList());
            }
            case "month" -> {
                List<Object[]> rows = filtered
                        ? requestStatisticsRepository.findMonthlyRequestCountsAndStatusCodeBetween(startDate, range[0], range[1])
                        : requestStatisticsRepository.findMonthlyRequestCounts(startDate);
                yield rows.stream()
                        .map(r -> {
                            LocalDateTime dt = (LocalDateTime) r[0];
                            String label = dt.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                            return new TimelineStat(label, ((Number) r[1]).longValue());
                        })
                        .collect(Collectors.toList());
            }
            default -> {
                // day
                List<Object[]> rows = filtered
                        ? requestStatisticsRepository.findDailyRequestCountsAndStatusCodeBetween(startDate, range[0], range[1])
                        : requestStatisticsRepository.findDailyRequestCounts(startDate);
                yield rows.stream()
                        .map(r -> {
                            LocalDate date = ((Date) r[0]).toLocalDate();
                            String label = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                            return new TimelineStat(label, ((Number) r[1]).longValue());
                        })
                        .collect(Collectors.toList());
            }
        };
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
