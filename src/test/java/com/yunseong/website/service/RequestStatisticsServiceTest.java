package com.yunseong.website.service;

import com.yunseong.website.domain.RequestStatistics;
import com.yunseong.website.repository.RequestStatisticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestStatisticsServiceTest {

    @Mock
    private RequestStatisticsRepository requestStatisticsRepository;

    @InjectMocks
    private RequestStatisticsService requestStatisticsService;

    @BeforeEach
    void setUp() {
        requestStatisticsService = new RequestStatisticsService(requestStatisticsRepository);
    }

    @Test
    void recordRequest_WithPublicUrl_RecordsStatistics() {
        // When
        requestStatisticsService.recordRequest("/public/memos/1", "GET", "https://example.com", "Mozilla/5.0");
        requestStatisticsService.recordRequest("/public/memos/1", "GET", "https://example.com", "Mozilla/5.0");
        requestStatisticsService.recordRequest("/public/memos/2", "GET", null, "Chrome/91.0");

        // Then - verify by persisting
        requestStatisticsService.persistStatistics();
        
        ArgumentCaptor<List<RequestStatistics>> captor = ArgumentCaptor.forClass(List.class);
        verify(requestStatisticsRepository, times(1)).saveAll(captor.capture());
        
        List<RequestStatistics> savedStats = captor.getValue();
        assertEquals(3, savedStats.size());
    }

    @Test
    void recordRequest_WithNonPublicUrl_DoesNotRecordStatistics() {
        // When
        requestStatisticsService.recordRequest("/admin/console", "GET", null, null);
        requestStatisticsService.recordRequest("/", "GET", null, null);

        // Then - verify by persisting
        requestStatisticsService.persistStatistics();
        
        verify(requestStatisticsRepository, never()).saveAll(any());
    }

    @Test
    void recordRequest_WithNullUri_DoesNotThrowException() {
        // When/Then
        assertDoesNotThrow(() -> requestStatisticsService.recordRequest(null, "GET", null, null));
    }

    @Test
    void persistStatistics_WithNoData_DoesNotSave() {
        // When
        requestStatisticsService.persistStatistics();

        // Then
        verify(requestStatisticsRepository, never()).saveAll(any());
    }

    @Test
    void persistStatistics_SavesCorrectData() {
        // Given
        requestStatisticsService.recordRequest("/public/memos/1", "GET", "https://referer.com", "Mozilla/5.0");
        requestStatisticsService.recordRequest("/public/memos/1", "GET", "https://another-referer.com", "Chrome/91.0");
        requestStatisticsService.recordRequest("/public/memos/1", "GET", "https://last-referer.com", "Safari/14.0");

        // When
        requestStatisticsService.persistStatistics();

        // Then
        ArgumentCaptor<List<RequestStatistics>> captor = ArgumentCaptor.forClass(List.class);
        verify(requestStatisticsRepository, times(1)).saveAll(captor.capture());
        
        List<RequestStatistics> savedStats = captor.getValue();
        assertEquals(3, savedStats.size());
        
        // Verify all three requests are saved individually
        assertEquals("/public/memos/1", savedStats.get(0).getUri());
        assertEquals("GET", savedStats.get(0).getMethod());
        assertEquals("https://referer.com", savedStats.get(0).getReferer());
        assertEquals("Mozilla/5.0", savedStats.get(0).getUserAgent());
        
        assertEquals("/public/memos/1", savedStats.get(1).getUri());
        assertEquals("https://another-referer.com", savedStats.get(1).getReferer());
        assertEquals("Chrome/91.0", savedStats.get(1).getUserAgent());
        
        assertEquals("/public/memos/1", savedStats.get(2).getUri());
        assertEquals("https://last-referer.com", savedStats.get(2).getReferer());
        assertEquals("Safari/14.0", savedStats.get(2).getUserAgent());
    }

    @Test
    void getStatisticsForLastDays_ReturnsStatistics() {
        // Given
        RequestStatistics stat1 = new RequestStatistics("/public/memos/1", "GET", null, "Mozilla/5.0");
        RequestStatistics stat2 = new RequestStatistics("/public/memos/2", "GET", null, "Chrome/91.0");
        List<RequestStatistics> mockStats = Arrays.asList(stat1, stat2);
        
        when(requestStatisticsRepository.findByCreatedAtAfter(any(LocalDateTime.class)))
                .thenReturn(mockStats);

        // When
        List<RequestStatistics> result = requestStatisticsService.getStatisticsForLastDays(7);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(requestStatisticsRepository, times(1)).findByCreatedAtAfter(any(LocalDateTime.class));
    }

    @Test
    void getTopUrisForLastDays_ReturnsTopUris() {
        // Given
        Object[] uri1 = {"/public/memos/1", 10L};
        Object[] uri2 = {"/public/memos/2", 5L};
        List<Object[]> mockResults = Arrays.asList(uri1, uri2);
        
        when(requestStatisticsRepository.findTopUrisByRequestCount(any(LocalDateTime.class)))
                .thenReturn(mockResults);

        // When
        Map<String, Long> result = requestStatisticsService.getTopUrisForLastDays(7);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(10L, result.get("/public/memos/1"));
        assertEquals(5L, result.get("/public/memos/2"));
    }

    @Test
    void getTotalRequestsForLastDays_ReturnsTotal() {
        // Given
        RequestStatistics stat1 = new RequestStatistics("/public/memos/1", "GET", null, "Mozilla/5.0");
        RequestStatistics stat2 = new RequestStatistics("/public/memos/2", "GET", null, "Chrome/91.0");
        List<RequestStatistics> mockStats = Arrays.asList(stat1, stat2);
        
        when(requestStatisticsRepository.findByCreatedAtAfter(any(LocalDateTime.class)))
                .thenReturn(mockStats);

        // When
        long total = requestStatisticsService.getTotalRequestsForLastDays(7);

        // Then
        assertEquals(2L, total); // Now returns count of records, not sum of request_count
    }
}
