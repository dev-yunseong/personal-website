package com.yunseong.website.service;

import com.yunseong.website.domain.RequestStatistics;
import com.yunseong.website.repository.RequestStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final RequestStatisticsRepository requestStatisticsRepository;
    
    // In-memory storage for request statistics
    private final Queue<RequestStatistics> requestQueue = new ConcurrentLinkedDeque<>();

    public void recordRequest(String uri, String method, String referer, String userAgent) {
        // Only track requests starting with /public/
        if (uri != null && uri.startsWith("/public/")) {
            RequestStatistics stats = new RequestStatistics(uri, method, referer, userAgent);
            requestQueue.add(stats);
            log.debug("Recorded request: {} {} (total in memory: {})", method, uri, requestQueue.size());
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
        return stats.size();
    }
}
