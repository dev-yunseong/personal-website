package dev.yunseong.website.manage.service;

import dev.yunseong.website.manage.domain.PageSessionCount;
import dev.yunseong.website.manage.domain.VisitorMetrics;
import dev.yunseong.website.manage.repository.VisitorSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitorSessionServiceTest {

    @Mock
    private VisitorSessionRepository visitorSessionRepository;

    @Test
    void derivesRatesFromSessionSummary() {
        // 8 sessions, 5 visitors, 2 returning, 20 page views, 2 single-page sessions
        stubSummary(new Object[]{8L, 5L, 2L, 20L, 2L, 95.4d});
        stubPages(List.of(
                new Object[]{"/", 5L, 1L},
                new Object[]{"/public/memos/1", 3L, 6L}));

        VisitorMetrics metrics = new VisitorSessionService(visitorSessionRepository)
                .getVisitorMetricsForLastDays(7, false);

        assertEquals(5L, metrics.uniqueVisitors());
        assertEquals(8L, metrics.sessions());
        assertEquals(20L, metrics.pageViews());
        assertEquals(2.5d, metrics.pagesPerSession());
        assertEquals(0.25d, metrics.singlePageSessionRate());
        assertEquals(0.4d, metrics.returningVisitorRate());
        assertEquals(95L, metrics.medianSessionSeconds());
    }

    @Test
    void ranksEntryAndExitPagesIndependently() {
        stubSummary(new Object[]{8L, 5L, 2L, 20L, 2L, 10d});
        stubPages(List.of(
                new Object[]{"/", 5L, 1L},
                new Object[]{"/public/memos/1", 3L, 6L},
                new Object[]{"/public/memos/2", 0L, 1L}));

        VisitorMetrics metrics = new VisitorSessionService(visitorSessionRepository)
                .getVisitorMetricsForLastDays(7, false);

        assertEquals(List.of(new PageSessionCount("/", 5L), new PageSessionCount("/public/memos/1", 3L)),
                metrics.topEntryPages());
        assertEquals(new PageSessionCount("/public/memos/1", 6L), metrics.topExitPages().get(0));
        // a page that never ended a session must not appear in the exit list
        assertTrue(metrics.topEntryPages().stream().noneMatch(page -> page.uri().equals("/public/memos/2")));
    }

    @Test
    void emptyPeriodReturnsZeroesInsteadOfDividingByZero() {
        stubSummary(new Object[]{0L, 0L, 0L, null, null, null});
        stubPages(List.of());

        VisitorMetrics metrics = new VisitorSessionService(visitorSessionRepository)
                .getVisitorMetricsForLastDays(30, false);

        assertEquals(0L, metrics.sessions());
        assertEquals(0L, metrics.pageViews());
        assertEquals(0d, metrics.pagesPerSession());
        assertEquals(0d, metrics.singlePageSessionRate());
        assertEquals(0d, metrics.returningVisitorRate());
        assertEquals(0L, metrics.medianSessionSeconds());
        assertTrue(metrics.topEntryPages().isEmpty());
        assertTrue(metrics.topExitPages().isEmpty());
    }

    private void stubSummary(Object[] row) {
        when(visitorSessionRepository.summariseSessions(any(LocalDateTime.class), eq(false)))
                .thenReturn(List.<Object[]>of(row));
    }

    private void stubPages(List<Object[]> rows) {
        when(visitorSessionRepository.countEntryAndExitPages(any(LocalDateTime.class), eq(false)))
                .thenReturn(rows);
    }
}
