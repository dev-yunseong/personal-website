package dev.yunseong.website.manage.service;

import dev.yunseong.website.manage.domain.RefererBreakdown;
import dev.yunseong.website.manage.domain.RefererGroupCount;
import dev.yunseong.website.manage.domain.RefererStat;
import dev.yunseong.website.manage.repository.RefererStatisticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefererStatisticsServiceTest {

    @Mock
    private RefererStatisticsRepository refererStatisticsRepository;

    private RefererStatisticsService refererStatisticsService;

    @BeforeEach
    void setUp() {
        refererStatisticsService =
                new RefererStatisticsService(refererStatisticsRepository, "https://yunseong.dev");
    }

    private void givenGroups(RefererGroupCount... groups) {
        when(refererStatisticsRepository.countByRefererAndBot(any(LocalDateTime.class)))
                .thenReturn(List.of(groups));
    }

    private static Map<String, Long> byLabel(List<RefererStat> stats) {
        return stats.stream().collect(Collectors.toMap(RefererStat::label, RefererStat::requestCount));
    }

    @Test
    void getRefererBreakdown_GroupsHostsIntoChannels() {
        givenGroups(
                new RefererGroupCount("https://www.google.com/search?q=x", false, 7L),
                new RefererGroupCount("https://google.co.kr/", false, 3L),
                new RefererGroupCount("https://news.ycombinator.com/item?id=1", false, 5L),
                new RefererGroupCount("https://blog.example.com/post", false, 2L),
                new RefererGroupCount("https://yunseong.dev/public/memos", false, 11L),
                new RefererGroupCount(null, false, 40L));

        RefererBreakdown breakdown = refererStatisticsService.getRefererBreakdownForLastDays(7, true, 0);

        assertEquals(Map.of(
                "Search", 10L,
                "Social & Community", 5L,
                "Other Referral", 2L,
                "Internal", 11L,
                "Direct", 40L), byLabel(breakdown.channels()));
    }

    @Test
    void getRefererBreakdown_WithNoData_StillReportsEveryChannelIncludingDirect() {
        givenGroups();

        RefererBreakdown breakdown = refererStatisticsService.getRefererBreakdownForLastDays(7, true, 0);

        assertEquals(List.of("Search", "Social & Community", "Other Referral", "Internal", "Direct"),
                breakdown.channels().stream().map(RefererStat::label).toList());
        assertTrue(breakdown.channels().stream().allMatch(stat -> stat.requestCount() == 0L));
        assertTrue(breakdown.domains().getContent().isEmpty());
    }

    @Test
    void getRefererBreakdown_ExcludingBots_DropsBotRows() {
        givenGroups(
                new RefererGroupCount("https://www.google.com/", false, 4L),
                new RefererGroupCount("https://www.google.com/", true, 96L),
                new RefererGroupCount(null, true, 500L));

        RefererBreakdown breakdown = refererStatisticsService.getRefererBreakdownForLastDays(7, true, 0);

        assertEquals(4L, byLabel(breakdown.channels()).get("Search"));
        assertEquals(0L, byLabel(breakdown.channels()).get("Direct"));
        assertEquals(List.of(new RefererStat("www.google.com", 4L)), breakdown.domains().getContent());
    }

    @Test
    void getRefererBreakdown_IncludingBots_CountsBotRows() {
        givenGroups(
                new RefererGroupCount("https://www.google.com/", false, 4L),
                new RefererGroupCount("https://www.google.com/", true, 96L),
                new RefererGroupCount(null, true, 500L));

        RefererBreakdown breakdown = refererStatisticsService.getRefererBreakdownForLastDays(7, false, 0);

        assertEquals(100L, byLabel(breakdown.channels()).get("Search"));
        assertEquals(500L, byLabel(breakdown.channels()).get("Direct"));
        assertEquals(List.of(new RefererStat("www.google.com", 100L)), breakdown.domains().getContent());
    }

    @Test
    void getRefererBreakdown_DomainList_ExcludesInternalAndDirectAndSortsByCountDesc() {
        givenGroups(
                new RefererGroupCount("https://blog.example.com/a", false, 2L),
                new RefererGroupCount("https://www.google.com/", false, 9L),
                new RefererGroupCount("https://t.co/abc", false, 5L),
                new RefererGroupCount("https://yunseong.dev/public/memos", false, 100L),
                new RefererGroupCount(null, false, 200L));

        RefererBreakdown breakdown = refererStatisticsService.getRefererBreakdownForLastDays(7, true, 0);

        assertEquals(List.of(
                new RefererStat("www.google.com", 9L),
                new RefererStat("t.co", 5L),
                new RefererStat("blog.example.com", 2L)), breakdown.domains().getContent());
        assertEquals(3L, breakdown.domains().getTotalElements());
    }

    @Test
    void getRefererBreakdown_PaginatesDomainsTenPerPage() {
        givenGroups(twelveExternalDomains());

        RefererBreakdown firstPage = refererStatisticsService.getRefererBreakdownForLastDays(7, true, 0);
        RefererBreakdown secondPage = refererStatisticsService.getRefererBreakdownForLastDays(7, true, 1);

        assertEquals(12L, firstPage.domains().getTotalElements());
        assertEquals(2, firstPage.domains().getTotalPages());
        assertEquals(10, firstPage.domains().getContent().size());
        assertTrue(firstPage.domains().isFirst());
        assertFalse(firstPage.domains().isLast());

        assertEquals(2, secondPage.domains().getContent().size());
        assertEquals(1, secondPage.domains().getNumber());
        assertTrue(secondPage.domains().isLast());
        // 12 domains counted 12 down to 1, so the last page holds the two smallest.
        assertEquals(List.of(new RefererStat("site11.example.com", 2L), new RefererStat("site12.example.com", 1L)),
                secondPage.domains().getContent());
    }

    @Test
    void getRefererBreakdown_WithNegativePage_FallsBackToFirstPage() {
        givenGroups(twelveExternalDomains());

        RefererBreakdown breakdown = refererStatisticsService.getRefererBreakdownForLastDays(7, true, -3);

        assertEquals(0, breakdown.domains().getNumber());
        assertEquals(10, breakdown.domains().getContent().size());
    }

    @Test
    void getRefererBreakdown_WithPageBeyondEnd_ReturnsEmptyContent() {
        givenGroups(twelveExternalDomains());

        RefererBreakdown breakdown = refererStatisticsService.getRefererBreakdownForLastDays(7, true, 9);

        assertTrue(breakdown.domains().getContent().isEmpty());
        assertEquals(12L, breakdown.domains().getTotalElements());
    }

    @Test
    void getRefererBreakdown_WithoutConfiguredBaseUrl_HasNoInternalChannel() {
        RefererStatisticsService serviceWithoutBaseUrl =
                new RefererStatisticsService(refererStatisticsRepository, "");
        givenGroups(new RefererGroupCount("https://yunseong.dev/public/memos", false, 6L));

        RefererBreakdown breakdown = serviceWithoutBaseUrl.getRefererBreakdownForLastDays(7, true, 0);

        assertEquals(6L, byLabel(breakdown.channels()).get("Other Referral"));
        assertEquals(0L, byLabel(breakdown.channels()).get("Internal"));
    }

    private static RefererGroupCount[] twelveExternalDomains() {
        List<RefererGroupCount> groups = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            groups.add(new RefererGroupCount("https://site%02d.example.com/".formatted(i), false, 13L - i));
        }
        return groups.toArray(RefererGroupCount[]::new);
    }

    @Test
    void getRefererBreakdown_MergesDifferentPathsOnTheSameHost() {
        givenGroups(
                new RefererGroupCount("https://blog.example.com/a", false, 2L),
                new RefererGroupCount("https://blog.example.com/b", false, 3L),
                new RefererGroupCount("blog.example.com/c", false, 4L));

        RefererBreakdown breakdown = refererStatisticsService.getRefererBreakdownForLastDays(7, true, 0);

        assertEquals(List.of(new RefererStat("blog.example.com", 9L)), breakdown.domains().getContent());
    }

    @Test
    void getRefererBreakdown_QueriesFromTheRequestedPeriodStart() {
        givenGroups();
        LocalDateTime before = LocalDateTime.now().minusDays(30);

        refererStatisticsService.getRefererBreakdownForLastDays(30, true, 0);

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(refererStatisticsRepository).countByRefererAndBot(captor.capture());
        assertFalse(captor.getValue().isBefore(before.minusSeconds(5)));
        assertTrue(captor.getValue().isBefore(LocalDateTime.now().minusDays(29)));
    }
}
