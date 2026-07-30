package dev.yunseong.website.manage.service;

import dev.yunseong.website.manage.domain.PageSessionCount;
import dev.yunseong.website.manage.domain.VisitorMetrics;
import dev.yunseong.website.manage.repository.VisitorSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VisitorSessionService {

    private static final int TOP_PAGE_LIMIT = 10;
    private static final int ENTRY_COUNT_COLUMN = 1;
    private static final int EXIT_COUNT_COLUMN = 2;

    private final VisitorSessionRepository visitorSessionRepository;

    @Transactional(readOnly = true)
    public VisitorMetrics getVisitorMetricsForLastDays(int days, boolean includeBots) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        List<Object[]> summary = visitorSessionRepository.summariseSessions(startDate, includeBots);
        Object[] row = summary.isEmpty() ? new Object[6] : summary.get(0);
        long sessions = toLong(row[0]);
        long uniqueVisitors = toLong(row[1]);
        long returningVisitors = toLong(row[2]);
        long pageViews = toLong(row[3]);
        long singlePageSessions = toLong(row[4]);
        long medianSessionSeconds = Math.round(toDouble(row[5]));

        List<Object[]> pages = visitorSessionRepository.countEntryAndExitPages(startDate, includeBots);

        return new VisitorMetrics(
                uniqueVisitors,
                sessions,
                pageViews,
                ratio(pageViews, sessions),
                ratio(singlePageSessions, sessions),
                medianSessionSeconds,
                ratio(returningVisitors, uniqueVisitors),
                topPages(pages, ENTRY_COUNT_COLUMN),
                topPages(pages, EXIT_COUNT_COLUMN));
    }

    /**
     * Sorts the entry/exit rows by the requested count column and keeps the head.
     * The list is one row per distinct page in the period, so sorting in memory
     * stays cheap while both top lists come from a single query.
     */
    private static List<PageSessionCount> topPages(List<Object[]> rows, int countColumn) {
        return rows.stream()
                .map(row -> new PageSessionCount((String) row[0], toLong(row[countColumn])))
                .filter(page -> page.sessionCount() > 0)
                .sorted(Comparator.comparingLong(PageSessionCount::sessionCount).reversed()
                        .thenComparing(PageSessionCount::uri))
                .limit(TOP_PAGE_LIMIT)
                .toList();
    }

    private static double ratio(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }

    private static long toLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private static double toDouble(Object value) {
        return value == null ? 0.0 : ((Number) value).doubleValue();
    }
}
