package com.yunseong.website.service;

import com.yunseong.website.domain.RequestStatistics;
import com.yunseong.website.repository.RequestStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestStatisticsService {
    private final RequestStatisticsRepository requestStatisticsRepository;
    
    // In-memory storage for request statistics
    private final Map<String, RequestData> requestDataMap = new ConcurrentHashMap<>();

    public void recordRequest(String uri, String method, String referer, String userAgent) {
        // Only track requests starting with /public/
        if (uri != null && uri.startsWith("/public/")) {
            String key = method + ":" + uri;
            requestDataMap.compute(key, (k, data) -> {
                if (data == null) {
                    data = new RequestData();
                }
                data.count++;
                data.referer = referer; // Store the most recent referer
                data.userAgent = userAgent; // Store the most recent user-agent
                return data;
            });
            log.debug("Recorded request: {} (total: {})", key, requestDataMap.get(key).count);
        }
    }

    private static class RequestData {
        long count = 0;
        String referer;
        String userAgent;
    }

    @Scheduled(fixedRate = 300000) // 5 minutes = 300000 milliseconds
    @Transactional
    public void persistStatistics() {
        if (requestDataMap.isEmpty()) {
            log.debug("No statistics to persist");
            return;
        }

        log.info("Persisting {} request statistics to database", requestDataMap.size());
        
        // Create a snapshot of current statistics and clear the map
        Map<String, RequestData> snapshot = new ConcurrentHashMap<>(requestDataMap);
        requestDataMap.clear();

        // Persist to database
        for (Map.Entry<String, RequestData> entry : snapshot.entrySet()) {
            String[] parts = entry.getKey().split(":", 2);
            if (parts.length == 2) {
                String method = parts[0];
                String uri = parts[1];
                RequestData data = entry.getValue();
                
                RequestStatistics stats = new RequestStatistics(uri, method, data.count, data.referer, data.userAgent);
                requestStatisticsRepository.save(stats);
            }
        }

        log.info("Successfully persisted {} statistics", snapshot.size());
    }

    @Transactional(readOnly = true)
    public List<RequestStatistics> getStatisticsForLastDays(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        return requestStatisticsRepository.findByCreatedAtAfter(startDate);
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
    public long getTotalRequestsForLastDays(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<RequestStatistics> stats = requestStatisticsRepository.findByCreatedAtAfter(startDate);
        return stats.stream().mapToLong(RequestStatistics::getRequestCount).sum();
    }
}
