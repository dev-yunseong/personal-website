package dev.yunseong.website.manage.service;

import dev.yunseong.website.manage.domain.RequestStatistics;
import dev.yunseong.website.manage.domain.GeoLocation;
import dev.yunseong.website.manage.domain.TimelineStat;
import dev.yunseong.website.manage.domain.UriStat;
import dev.yunseong.website.manage.repository.RequestStatisticsRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestStatisticsServiceTest {

    @Mock
    private RequestStatisticsRepository requestStatisticsRepository;

    @Mock
    private GeoIpLocationResolver geoIpLocationResolver;

    @InjectMocks
    private RequestStatisticsService requestStatisticsService;

    @BeforeEach
    void setUp() {
        requestStatisticsService = new RequestStatisticsService(requestStatisticsRepository, geoIpLocationResolver);
    }

    @Test
    void recordRequest_WithPublicUrl_RecordsStatistics() {
        // When
        requestStatisticsService.recordRequest("/public/memos/1", "GET", "https://example.com", "Mozilla/5.0", "1.1.1.1", 200, 12);
        requestStatisticsService.recordRequest("/public/memos/1", "GET", "https://example.com", "Mozilla/5.0", "1.1.1.1", 200, 12);
        requestStatisticsService.recordRequest("/public/memos/2", "GET", null, "Chrome/91.0", "1.1.1.1", 200, 12);

        // Then - verify by persisting
        requestStatisticsService.persistStatistics();

        ArgumentCaptor<List<RequestStatistics>> captor = ArgumentCaptor.forClass(List.class);
        verify(requestStatisticsRepository, times(1)).saveAll(captor.capture());

        List<RequestStatistics> savedStats = captor.getValue();
        assertEquals(3, savedStats.size());
    }

    @Test
    void recordRequest_WithWhitelistedPublicSurface_RecordsStatistics() {
        // When
        requestStatisticsService.recordRequest("/", "GET", null, "Mozilla/5.0", "1.1.1.1", 200, 5);
        requestStatisticsService.recordRequest("/public/search", "GET", null, "Mozilla/5.0", "1.1.1.1", 200, 5);
        requestStatisticsService.recordRequest("/api/public/search/suggestions", "GET", null, "Mozilla/5.0", "1.1.1.1", 200, 5);

        // Then
        requestStatisticsService.persistStatistics();

        ArgumentCaptor<List<RequestStatistics>> captor = ArgumentCaptor.forClass(List.class);
        verify(requestStatisticsRepository, times(1)).saveAll(captor.capture());
        assertEquals(List.of("/", "/public/search", "/api/public/search/suggestions"),
                captor.getValue().stream().map(RequestStatistics::getUri).toList());
    }

    @Test
    void recordRequest_WithNonPublicUrl_DoesNotRecordStatistics() {
        // When
        requestStatisticsService.recordRequest("/admin/console", "GET", null, "Mozilla/5.0", "1.1.1.1", 200, 5);
        requestStatisticsService.recordRequest("/api/admin/console/summary", "GET", null, "Mozilla/5.0", "1.1.1.1", 200, 5);
        requestStatisticsService.recordRequest("/css/main.css", "GET", null, "Mozilla/5.0", "1.1.1.1", 200, 5);
        requestStatisticsService.recordRequest("/js/chat.js", "GET", null, "Mozilla/5.0", "1.1.1.1", 200, 5);
        requestStatisticsService.recordRequest("/images/logo.png", "GET", null, "Mozilla/5.0", "1.1.1.1", 200, 5);
        requestStatisticsService.recordRequest("/favicon.ico", "GET", null, "Mozilla/5.0", "1.1.1.1", 200, 5);
        requestStatisticsService.recordRequest("/robots.txt", "GET", null, "Mozilla/5.0", "1.1.1.1", 200, 5);
        requestStatisticsService.recordRequest("/sitemap.xml", "GET", null, "Mozilla/5.0", "1.1.1.1", 200, 5);
        requestStatisticsService.recordRequest("/.well-known/security.txt", "GET", null, "Mozilla/5.0", "1.1.1.1", 200, 5);

        // Then - verify by persisting
        requestStatisticsService.persistStatistics();

        verify(requestStatisticsRepository, never()).saveAll(any());
    }

    @Test
    void recordRequest_WithNullUri_DoesNotThrowException() {
        // When/Then
        assertDoesNotThrow(() -> requestStatisticsService.recordRequest(null, "GET", null, "Mozilla/5.0", "1.1.1.1", 200, 5));
    }

    @Test
    void recordRequest_StoresBotFlagAndDuration() {
        // When
        requestStatisticsService.recordRequest("/", "GET", null,
                "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)", "1.1.1.1", 200, 42);
        requestStatisticsService.recordRequest("/", "GET", null,
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15",
                "1.1.1.1", 200, 7);

        // Then
        requestStatisticsService.persistStatistics();

        ArgumentCaptor<List<RequestStatistics>> captor = ArgumentCaptor.forClass(List.class);
        verify(requestStatisticsRepository, times(1)).saveAll(captor.capture());

        List<RequestStatistics> savedStats = captor.getValue();
        assertTrue(savedStats.get(0).isBot());
        assertEquals(42, savedStats.get(0).getDurationMs());
        assertFalse(savedStats.get(1).isBot());
        assertEquals(7, savedStats.get(1).getDurationMs());
    }

    @Test
    void recordRequest_StoresResolvedCityLocation() {
        // Given
        when(geoIpLocationResolver.resolve("1.1.1.1"))
                .thenReturn(new GeoLocation("AU", 2158177L, "Melbourne", -37.814, 144.9633, 20));

        // When
        requestStatisticsService.recordRequest("/", "GET", null, "Mozilla/5.0", "1.1.1.1", 200, 5);
        requestStatisticsService.persistStatistics();

        // Then
        ArgumentCaptor<List<RequestStatistics>> captor = ArgumentCaptor.forClass(List.class);
        verify(requestStatisticsRepository, times(1)).saveAll(captor.capture());
        assertEquals("AU", captor.getValue().get(0).getCountryCode());
        assertEquals(2158177L, captor.getValue().get(0).getCityGeoNameId());
        assertEquals("Melbourne", captor.getValue().get(0).getCityName());
        assertEquals(-37.814, captor.getValue().get(0).getLatitude());
        assertEquals(20, captor.getValue().get(0).getAccuracyRadiusKm());
    }

    @Test
    void recordRequest_WithoutGeoDatabase_LeavesCountryCodeNull() {
        // Given - resolver with no database returns null for every address
        when(geoIpLocationResolver.resolve("1.1.1.1")).thenReturn(null);

        // When
        requestStatisticsService.recordRequest("/", "GET", null, "Mozilla/5.0", "1.1.1.1", 200, 5);
        requestStatisticsService.persistStatistics();

        // Then
        ArgumentCaptor<List<RequestStatistics>> captor = ArgumentCaptor.forClass(List.class);
        verify(requestStatisticsRepository, times(1)).saveAll(captor.capture());
        assertNull(captor.getValue().get(0).getCountryCode());
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
        requestStatisticsService.recordRequest("/public/memos/1", "GET", "https://referer.com", "Mozilla/5.0", "1.1.1.1", 200, 11);
        requestStatisticsService.recordRequest("/public/memos/1", "GET", "https://another-referer.com", "Chrome/91.0", "1.1.1.1", 200, 22);
        requestStatisticsService.recordRequest("/public/memos/1", "GET", "https://last-referer.com", "Safari/14.0", "1.1.1.1", 200, 33);

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
        RequestStatistics stat1 = new RequestStatistics("/public/memos/1", "GET", null, "Mozilla/5.0", "1.1.1.1");
        RequestStatistics stat2 = new RequestStatistics("/public/memos/2", "GET", null, "Chrome/91.0", "1.1.1.1");
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
    void getStatisticsForLastDays_WithPageable_ReturnsPage() {
        // Given
        RequestStatistics stat1 = new RequestStatistics("/public/memos/1", "GET", null, "Mozilla/5.0", "1.1.1.1");
        RequestStatistics stat2 = new RequestStatistics("/public/memos/2", "GET", null, "Chrome/91.0", "1.1.1.1");
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RequestStatistics> mockPage = new PageImpl<>(Arrays.asList(stat1, stat2), pageable, 2);

        when(requestStatisticsRepository.findByCreatedAtAfter(any(LocalDateTime.class), eq(pageable)))
                .thenReturn(mockPage);

        // When
        Page<RequestStatistics> result = requestStatisticsService.getStatisticsForLastDays(7, "", pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        verify(requestStatisticsRepository, times(1)).findByCreatedAtAfter(any(LocalDateTime.class), eq(pageable));
    }

    @Test
    void getStatisticsForLastDays_WithStatusFilter_UsesStatusQuery() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RequestStatistics> mockPage = new PageImpl<>(List.of(), pageable, 0);

        when(requestStatisticsRepository.findByCreatedAtAfterAndStatusCodeBetween(
                any(LocalDateTime.class), eq(200), eq(299), eq(pageable)))
                .thenReturn(mockPage);

        // When
        Page<RequestStatistics> result = requestStatisticsService.getStatisticsForLastDays(7, "2xx", pageable);

        // Then
        assertNotNull(result);
        verify(requestStatisticsRepository, times(1)).findByCreatedAtAfterAndStatusCodeBetween(
                any(LocalDateTime.class), eq(200), eq(299), eq(pageable));
        verify(requestStatisticsRepository, never()).findByCreatedAtAfter(any(LocalDateTime.class), any(Pageable.class));
    }

    @Test
    void getTotalRequestsForLastDays_UsesCountQuery() {
        // Given
        when(requestStatisticsRepository.countByCreatedAtAfter(any(LocalDateTime.class)))
                .thenReturn(5L);

        // When
        long total = requestStatisticsService.getTotalRequestsForLastDays(7, "");

        // Then
        assertEquals(5L, total);
        verify(requestStatisticsRepository, times(1)).countByCreatedAtAfter(any(LocalDateTime.class));
        verify(requestStatisticsRepository, never()).findByCreatedAtAfter(any(LocalDateTime.class));
    }

    @Test
    void getTotalRequestsForLastDays_WithStatusFilter_UsesStatusCountQuery() {
        // Given
        when(requestStatisticsRepository.countByCreatedAtAfterAndStatusCodeBetween(
                any(LocalDateTime.class), eq(400), eq(499)))
                .thenReturn(3L);

        // When
        long total = requestStatisticsService.getTotalRequestsForLastDays(7, "4xx");

        // Then
        assertEquals(3L, total);
        verify(requestStatisticsRepository, times(1)).countByCreatedAtAfterAndStatusCodeBetween(
                any(LocalDateTime.class), eq(400), eq(499));
        verify(requestStatisticsRepository, never()).countByCreatedAtAfter(any(LocalDateTime.class));
    }

    @Test
    void getTopUrisPageForLastDays_DefaultSort_ReturnsCountDescPage() {
        // Given
        UriStat row1 = new UriStat("/public/memos/1", 10L);
        UriStat row2 = new UriStat("/public/memos/2", 5L);
        Pageable pageable = PageRequest.of(0, 10);
        Page<UriStat> mockPage = new PageImpl<>(Arrays.asList(row1, row2), pageable, 2);

        when(requestStatisticsRepository.findTopUrisByRequestCount(any(LocalDateTime.class), eq(pageable)))
                .thenReturn(mockPage);

        // When
        Page<UriStat> result = requestStatisticsService.getTopUrisPageForLastDays(7, "count_desc", "", 0);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals("/public/memos/1", result.getContent().get(0).uri());
        assertEquals(10L, result.getContent().get(0).requestCount());
        verify(requestStatisticsRepository, times(1)).findTopUrisByRequestCount(any(LocalDateTime.class), eq(pageable));
    }

    @Test
    void getTopUrisPageForLastDays_CountAscSort_UsesCountAscQuery() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<UriStat> mockPage = new PageImpl<>(List.of(), pageable, 0);

        when(requestStatisticsRepository.findTopUrisByCountAsc(any(LocalDateTime.class), eq(pageable)))
                .thenReturn(mockPage);

        // When
        Page<UriStat> result = requestStatisticsService.getTopUrisPageForLastDays(7, "count_asc", "", 0);

        // Then
        assertNotNull(result);
        verify(requestStatisticsRepository, times(1)).findTopUrisByCountAsc(any(LocalDateTime.class), eq(pageable));
    }

    @Test
    void getTopUrisPageForLastDays_UriAscSort_UsesSortedByUriQuery() {
        // Given
        Pageable uriAscPageable = PageRequest.of(0, 10, Sort.by("uri").ascending());
        Page<UriStat> mockPage = new PageImpl<>(List.of(), uriAscPageable, 0);

        when(requestStatisticsRepository.findTopUrisSortedByUri(any(LocalDateTime.class), eq(uriAscPageable)))
                .thenReturn(mockPage);

        // When
        Page<UriStat> result = requestStatisticsService.getTopUrisPageForLastDays(7, "key_asc", "", 0);

        // Then
        assertNotNull(result);
        verify(requestStatisticsRepository, times(1)).findTopUrisSortedByUri(any(LocalDateTime.class), eq(uriAscPageable));
    }

    @Test
    void getTopUrisPageForLastDays_WithStatusFilter_UsesFilteredCountDescQuery() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<UriStat> mockPage = new PageImpl<>(List.of(), pageable, 0);

        when(requestStatisticsRepository.findTopUrisByRequestCountAndStatusCodeBetween(
                any(LocalDateTime.class), eq(400), eq(499), eq(pageable)))
                .thenReturn(mockPage);

        // When
        Page<UriStat> result = requestStatisticsService.getTopUrisPageForLastDays(7, "count_desc", "4xx", 0);

        // Then
        assertNotNull(result);
        verify(requestStatisticsRepository, times(1)).findTopUrisByRequestCountAndStatusCodeBetween(
                any(LocalDateTime.class), eq(400), eq(499), eq(pageable));
        verify(requestStatisticsRepository, never()).findTopUrisByRequestCount(any(LocalDateTime.class), any(Pageable.class));
    }

    @Test
    void getTopUrisPageForLastDays_WithStatusFilterAndCountAsc_UsesFilteredCountAscQuery() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<UriStat> mockPage = new PageImpl<>(List.of(), pageable, 0);

        when(requestStatisticsRepository.findTopUrisByCountAscAndStatusCodeBetween(
                any(LocalDateTime.class), eq(200), eq(299), eq(pageable)))
                .thenReturn(mockPage);

        // When
        Page<UriStat> result = requestStatisticsService.getTopUrisPageForLastDays(7, "count_asc", "2xx", 0);

        // Then
        assertNotNull(result);
        verify(requestStatisticsRepository, times(1)).findTopUrisByCountAscAndStatusCodeBetween(
                any(LocalDateTime.class), eq(200), eq(299), eq(pageable));
    }

    @Test
    void getTopIpsPageForLastDays_DefaultSort_ReturnsCountDescPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<UriStat> mockPage = new PageImpl<>(List.of(new UriStat("1.1.1.1", 5L)), pageable, 1);

        when(requestStatisticsRepository.findTopIpsByRequestCount(any(LocalDateTime.class), eq(pageable)))
                .thenReturn(mockPage);

        // When
        Page<UriStat> result = requestStatisticsService.getTopIpsPageForLastDays(7, "count_desc", "", 0);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(requestStatisticsRepository, times(1)).findTopIpsByRequestCount(any(LocalDateTime.class), eq(pageable));
    }

    @Test
    void getTopIpsPageForLastDays_WithStatusFilter_UsesFilteredQuery() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<UriStat> mockPage = new PageImpl<>(List.of(), pageable, 0);

        when(requestStatisticsRepository.findTopIpsByRequestCountAndStatusCodeBetween(
                any(LocalDateTime.class), eq(200), eq(299), eq(pageable)))
                .thenReturn(mockPage);

        // When
        Page<UriStat> result = requestStatisticsService.getTopIpsPageForLastDays(7, "count_desc", "2xx", 0);

        // Then
        assertNotNull(result);
        verify(requestStatisticsRepository, times(1)).findTopIpsByRequestCountAndStatusCodeBetween(
                any(LocalDateTime.class), eq(200), eq(299), eq(pageable));
    }

    @Test
    void getTopUserAgentsPageForLastDays_DefaultSort_ReturnsCountDescPage() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<UriStat> mockPage = new PageImpl<>(List.of(new UriStat("Mozilla/5.0", 8L)), pageable, 1);

        when(requestStatisticsRepository.findTopUserAgentsByRequestCount(any(LocalDateTime.class), eq(pageable)))
                .thenReturn(mockPage);

        // When
        Page<UriStat> result = requestStatisticsService.getTopUserAgentsPageForLastDays(7, "count_desc", "", 0);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(requestStatisticsRepository, times(1)).findTopUserAgentsByRequestCount(any(LocalDateTime.class), eq(pageable));
    }

    @Test
    void getTopUserAgentsPageForLastDays_WithStatusFilter_UsesFilteredQuery() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<UriStat> mockPage = new PageImpl<>(List.of(), pageable, 0);

        when(requestStatisticsRepository.findTopUserAgentsByRequestCountAndStatusCodeBetween(
                any(LocalDateTime.class), eq(400), eq(499), eq(pageable)))
                .thenReturn(mockPage);

        // When
        Page<UriStat> result = requestStatisticsService.getTopUserAgentsPageForLastDays(7, "count_desc", "4xx", 0);

        // Then
        assertNotNull(result);
        verify(requestStatisticsRepository, times(1)).findTopUserAgentsByRequestCountAndStatusCodeBetween(
                any(LocalDateTime.class), eq(400), eq(499), eq(pageable));
    }

    @Test
    void getTimelineForLastDays_NoFilter_ReturnsAllDailyData() {
        // Given - repository now returns Object[] rows
        java.time.LocalDate date1 = LocalDate.of(2024, 1, 1);
        java.time.LocalDate date2 = LocalDate.of(2024, 1, 2);
        List<Object[]> mockRows = List.of(
                new Object[]{java.sql.Date.valueOf(date1), 10L},
                new Object[]{java.sql.Date.valueOf(date2), 20L}
        );
        when(requestStatisticsRepository.findDailyRequestCounts(any(LocalDateTime.class))).thenReturn(mockRows);

        // When
        List<TimelineStat> result = requestStatisticsService.getTimelineForLastDays(7, "");

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("2024-01-01", result.get(0).label());
        assertEquals(10L, result.get(0).count());
        verify(requestStatisticsRepository, times(1)).findDailyRequestCounts(any(LocalDateTime.class));
    }

    @Test
    void getTimelineForLastDays_WithStatusFilter_UsesFilteredQuery() {
        // Given
        java.time.LocalDate date1 = LocalDate.of(2024, 1, 1);
        List<Object[]> mockRows = List.<Object[]>of(new Object[]{java.sql.Date.valueOf(date1), 5L});
        when(requestStatisticsRepository.findDailyRequestCountsAndStatusCodeBetween(
                any(LocalDateTime.class), eq(200), eq(299))).thenReturn(mockRows);

        // When
        List<TimelineStat> result = requestStatisticsService.getTimelineForLastDays(7, "2xx");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("2024-01-01", result.get(0).label());
        verify(requestStatisticsRepository, times(1)).findDailyRequestCountsAndStatusCodeBetween(
                any(LocalDateTime.class), eq(200), eq(299));
        verify(requestStatisticsRepository, never()).findDailyRequestCounts(any(LocalDateTime.class));
    }

    @Test
    void getTimelineForLastDays_HourlyResolution_UsesHourlyQuery() {
        // Given
        java.time.LocalDateTime dt = java.time.LocalDateTime.of(2024, 1, 1, 10, 0);
        List<Object[]> mockRows = List.<Object[]>of(new Object[]{dt, 5L});
        when(requestStatisticsRepository.findHourlyRequestCounts(any(LocalDateTime.class))).thenReturn(mockRows);

        // When
        List<TimelineStat> result = requestStatisticsService.getTimelineForLastDays(1, "", "hour");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("2024-01-01 10:00", result.get(0).label());
        verify(requestStatisticsRepository, times(1)).findHourlyRequestCounts(any(LocalDateTime.class));
    }

    @Test
    void getTimelineForLastDays_WeeklyResolution_AggregatesDailyIntoWeeks() {
        // Given - two dates in the same week (Mon 2024-01-01 and Tue 2024-01-02)
        List<Object[]> mockRows = List.of(
                new Object[]{java.sql.Date.valueOf(LocalDate.of(2024, 1, 1)), 10L},
                new Object[]{java.sql.Date.valueOf(LocalDate.of(2024, 1, 2)), 20L}
        );
        when(requestStatisticsRepository.findDailyRequestCounts(any(LocalDateTime.class))).thenReturn(mockRows);

        // When
        List<TimelineStat> result = requestStatisticsService.getTimelineForLastDays(7, "", "week");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size()); // both dates map to same ISO week start
        assertEquals(30L, result.get(0).count());
    }

    @Test
    void getTimelineForLastDays_MonthlyResolution_UsesMonthlyQuery() {
        // Given
        java.time.LocalDateTime dt = java.time.LocalDateTime.of(2024, 1, 1, 0, 0);
        List<Object[]> mockRows = List.<Object[]>of(new Object[]{dt, 50L});
        when(requestStatisticsRepository.findMonthlyRequestCounts(any(LocalDateTime.class))).thenReturn(mockRows);

        // When
        List<TimelineStat> result = requestStatisticsService.getTimelineForLastDays(30, "", "month");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("2024-01", result.get(0).label());
        verify(requestStatisticsRepository, times(1)).findMonthlyRequestCounts(any(LocalDateTime.class));
    }

}
