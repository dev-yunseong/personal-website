package dev.yunseong.website.manage.service;

import dev.yunseong.website.manage.domain.NotFoundStat;
import dev.yunseong.website.manage.repository.NotFoundStatisticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotFoundReportServiceTest {

    private static final String INTERNAL_PATTERN = "%//yunseong.dev/%";

    @Mock
    private NotFoundStatisticsRepository notFoundStatisticsRepository;

    private NotFoundReportService notFoundReportService;

    @BeforeEach
    void setUp() {
        notFoundReportService = new NotFoundReportService(notFoundStatisticsRepository, "https://yunseong.dev");
    }

    @Test
    void getNotFoundReportForLastDays_DefaultSort_UsesOccurrencesQueryAndCountForTotal() {
        NotFoundStat stat = new NotFoundStat("/public/memos/999", 12L,
                LocalDateTime.of(2026, 7, 1, 9, 0), LocalDateTime.of(2026, 7, 20, 18, 30),
                "https://yunseong.dev/public/memos", 9L, 2L, 1L);
        when(notFoundStatisticsRepository.findNotFoundStatsByOccurrences(
                any(LocalDateTime.class), anyCollection(), anyString(), any(Pageable.class)))
                .thenReturn(List.of(stat));
        when(notFoundStatisticsRepository.countNotFoundUris(any(LocalDateTime.class), anyCollection()))
                .thenReturn(37L);

        Page<NotFoundStat> result = notFoundReportService.getNotFoundReportForLastDays(7, "count_desc", false, 0);

        assertEquals(List.of(stat), result.getContent());
        assertEquals(37L, result.getTotalElements());
        assertEquals(NotFoundReportService.PAGE_SIZE, result.getSize());
        verify(notFoundStatisticsRepository, never()).findNotFoundStatsByLastSeen(
                any(LocalDateTime.class), anyCollection(), anyString(), any(Pageable.class));
    }

    @Test
    void getNotFoundReportForLastDays_RecentSort_UsesLastSeenQuery() {
        when(notFoundStatisticsRepository.findNotFoundStatsByLastSeen(
                any(LocalDateTime.class), anyCollection(), anyString(), any(Pageable.class)))
                .thenReturn(List.of());

        notFoundReportService.getNotFoundReportForLastDays(30, "recent", false, 0);

        verify(notFoundStatisticsRepository).findNotFoundStatsByLastSeen(
                any(LocalDateTime.class), anyCollection(), anyString(), any(Pageable.class));
        verify(notFoundStatisticsRepository, never()).findNotFoundStatsByOccurrences(
                any(LocalDateTime.class), anyCollection(), anyString(), any(Pageable.class));
    }

    @Test
    void getNotFoundReportForLastDays_ExcludesBotsByDefault() {
        when(notFoundStatisticsRepository.findNotFoundStatsByOccurrences(
                any(LocalDateTime.class), anyCollection(), anyString(), any(Pageable.class)))
                .thenReturn(List.of());

        notFoundReportService.getNotFoundReportForLastDays(7, "count_desc", false, 0);

        assertEquals(List.of(false), List.copyOf(capturedBotFlags()));
        verify(notFoundStatisticsRepository).countNotFoundUris(any(LocalDateTime.class), eq(List.of(false)));
    }

    @Test
    void getNotFoundReportForLastDays_IncludeBots_QueriesBothBotFlags() {
        when(notFoundStatisticsRepository.findNotFoundStatsByOccurrences(
                any(LocalDateTime.class), anyCollection(), anyString(), any(Pageable.class)))
                .thenReturn(List.of());

        notFoundReportService.getNotFoundReportForLastDays(7, "count_desc", true, 0);

        assertEquals(List.of(false, true), List.copyOf(capturedBotFlags()));
    }

    @Test
    void getNotFoundReportForLastDays_DerivesInternalRefererPatternFromBaseUrlHost() {
        when(notFoundStatisticsRepository.findNotFoundStatsByOccurrences(
                any(LocalDateTime.class), anyCollection(), anyString(), any(Pageable.class)))
                .thenReturn(List.of());

        notFoundReportService.getNotFoundReportForLastDays(7, "count_desc", false, 0);

        ArgumentCaptor<String> pattern = ArgumentCaptor.forClass(String.class);
        verify(notFoundStatisticsRepository).findNotFoundStatsByOccurrences(
                any(LocalDateTime.class), anyCollection(), pattern.capture(), any(Pageable.class));
        assertEquals(INTERNAL_PATTERN, pattern.getValue());
    }

    @Test
    void getNotFoundReportForLastDays_TranslatesDaysIntoStartDateAndPageIntoPageable() {
        when(notFoundStatisticsRepository.findNotFoundStatsByOccurrences(
                any(LocalDateTime.class), anyCollection(), anyString(), any(Pageable.class)))
                .thenReturn(List.of());
        LocalDateTime before = LocalDateTime.now();

        notFoundReportService.getNotFoundReportForLastDays(30, "count_desc", false, 2);

        ArgumentCaptor<LocalDateTime> startDate = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(notFoundStatisticsRepository).findNotFoundStatsByOccurrences(
                startDate.capture(), anyCollection(), anyString(), pageable.capture());
        assertTrue(!startDate.getValue().isBefore(before.minusDays(30).minusMinutes(1))
                        && !startDate.getValue().isAfter(LocalDateTime.now().minusDays(30)),
                "startDate should be about 30 days ago but was " + startDate.getValue());
        assertEquals(2, pageable.getValue().getPageNumber());
        assertEquals(NotFoundReportService.PAGE_SIZE, pageable.getValue().getPageSize());
    }

    private Collection<Boolean> capturedBotFlags() {
        ArgumentCaptor<Collection<Boolean>> botFlags = ArgumentCaptor.forClass(Collection.class);
        verify(notFoundStatisticsRepository).findNotFoundStatsByOccurrences(
                any(LocalDateTime.class), botFlags.capture(), anyString(), any(Pageable.class));
        return botFlags.getValue();
    }
}
