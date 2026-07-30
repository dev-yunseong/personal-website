package dev.yunseong.website.manage.service;

import dev.yunseong.website.manage.domain.TrafficDistribution;
import dev.yunseong.website.manage.domain.TrafficSlice;
import dev.yunseong.website.manage.domain.UriStat;
import dev.yunseong.website.manage.repository.TrafficDistributionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrafficDistributionServiceTest {

    private static final String IPHONE_USER_AGENT =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Mobile/15E148 Safari/604.1";
    private static final String WINDOWS_CHROME_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String MAC_CHROME_USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    @Mock
    private TrafficDistributionRepository trafficDistributionRepository;

    private TrafficDistributionService trafficDistributionService;

    @BeforeEach
    void setUp() {
        trafficDistributionService = new TrafficDistributionService(trafficDistributionRepository);
    }

    @Test
    void getDistributionForLastDays_FoldsHourBucketsOntoDayOfWeekAndHourGrid() {
        // 2024-01-01 is a Monday, 2024-01-07 a Sunday.
        stubHourBuckets(
                hourBucket(LocalDateTime.of(2024, 1, 1, 9, 0), 5L),
                hourBucket(LocalDateTime.of(2024, 1, 8, 9, 0), 3L),
                hourBucket(LocalDateTime.of(2024, 1, 7, 23, 0), 2L));
        stubUserAgents();

        TrafficDistribution distribution = trafficDistributionService.getDistributionForLastDays(30, false);

        List<List<Long>> grid = distribution.hourOfWeekCounts();
        assertEquals(7, grid.size());
        assertEquals(24, grid.get(0).size());
        assertEquals(8L, grid.get(0).get(9).longValue(), "two Mondays at 09:00 share one cell");
        assertEquals(2L, grid.get(6).get(23).longValue(), "Sunday 23:00");
        assertEquals(0L, grid.get(0).get(10).longValue());
        assertEquals(10L, grid.stream().flatMap(List::stream).mapToLong(Long::longValue).sum());
    }

    @Test
    void getDistributionForLastDays_MergesUserAgentsIntoSlicesOrderedByCount() {
        stubHourBuckets(hourBucket(LocalDateTime.of(2024, 1, 1, 9, 0), 13L));
        stubUserAgents(
                new UriStat(IPHONE_USER_AGENT, 3L),
                new UriStat(WINDOWS_CHROME_USER_AGENT, 7L),
                new UriStat(MAC_CHROME_USER_AGENT, 2L),
                new UriStat(null, 1L));

        TrafficDistribution distribution = trafficDistributionService.getDistributionForLastDays(7, false);

        assertEquals(List.of(new TrafficSlice("Desktop", 9L),
                        new TrafficSlice("Mobile", 3L),
                        new TrafficSlice("Unknown", 1L)),
                distribution.devices());
        assertEquals(List.of(new TrafficSlice("Chrome", 9L),
                        new TrafficSlice("Safari", 3L),
                        new TrafficSlice("Unknown", 1L)),
                distribution.browsers());
        assertEquals(List.of(new TrafficSlice("Windows", 7L),
                        new TrafficSlice("iOS", 3L),
                        new TrafficSlice("macOS", 2L),
                        new TrafficSlice("Unknown", 1L)),
                distribution.operatingSystems());
    }

    @Test
    void getDistributionForLastDays_PassesBotInclusionToBothQueries() {
        stubHourBuckets();
        stubUserAgents();

        trafficDistributionService.getDistributionForLastDays(7, true);

        verify(trafficDistributionRepository, times(1)).countByHour(any(LocalDateTime.class), eq(true));
        verify(trafficDistributionRepository, times(1)).countByUserAgent(any(LocalDateTime.class), eq(true));
    }

    @Test
    void getDistributionForLastDays_ReportsServerTimeZoneOfTheBuckets() {
        stubHourBuckets();
        stubUserAgents();

        TrafficDistribution distribution = trafficDistributionService.getDistributionForLastDays(7, false);

        assertEquals(ZoneId.systemDefault().getId(), distribution.timeZone());
    }

    private static Object[] hourBucket(LocalDateTime hour, long count) {
        return new Object[]{hour, count};
    }

    private void stubHourBuckets(Object[]... rows) {
        when(trafficDistributionRepository.countByHour(any(LocalDateTime.class), anyBoolean()))
                .thenReturn(List.of(rows));
    }

    private void stubUserAgents(UriStat... rows) {
        when(trafficDistributionRepository.countByUserAgent(any(LocalDateTime.class), anyBoolean()))
                .thenReturn(List.of(rows));
    }
}
