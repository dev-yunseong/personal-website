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
        
        ArgumentCaptor<RequestStatistics> captor = ArgumentCaptor.forClass(RequestStatistics.class);
        verify(requestStatisticsRepository, times(2)).save(captor.capture());
        
        List<RequestStatistics> savedStats = captor.getAllValues();
        assertEquals(2, savedStats.size());
    }

    @Test
    void recordRequest_WithNonPublicUrl_DoesNotRecordStatistics() {
        // When
        requestStatisticsService.recordRequest("/admin/console", "GET", null, null);
        requestStatisticsService.recordRequest("/", "GET", null, null);

        // Then - verify by persisting
        requestStatisticsService.persistStatistics();
        
        verify(requestStatisticsRepository, never()).save(any());
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
        verify(requestStatisticsRepository, never()).save(any());
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
        ArgumentCaptor<RequestStatistics> captor = ArgumentCaptor.forClass(RequestStatistics.class);
        verify(requestStatisticsRepository, times(1)).save(captor.capture());
        
        RequestStatistics saved = captor.getValue();
        assertEquals("/public/memos/1", saved.getUri());
        assertEquals("GET", saved.getMethod());
        assertEquals(3L, saved.getRequestCount());
        assertEquals("https://last-referer.com", saved.getReferer()); // Most recent referer
        assertEquals("Safari/14.0", saved.getUserAgent()); // Most recent user-agent
    }

    @Test
    void getStatisticsForLastDays_ReturnsStatistics() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        RequestStatistics stat1 = new RequestStatistics("/public/memos/1", "GET", 5L);
        RequestStatistics stat2 = new RequestStatistics("/public/memos/2", "GET", 3L);
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
        RequestStatistics stat1 = new RequestStatistics("/public/memos/1", "GET", 5L);
        RequestStatistics stat2 = new RequestStatistics("/public/memos/2", "GET", 3L);
        List<RequestStatistics> mockStats = Arrays.asList(stat1, stat2);
        
        when(requestStatisticsRepository.findByCreatedAtAfter(any(LocalDateTime.class)))
                .thenReturn(mockStats);

        // When
        long total = requestStatisticsService.getTotalRequestsForLastDays(7);

        // Then
        assertEquals(8L, total);
    }
}
