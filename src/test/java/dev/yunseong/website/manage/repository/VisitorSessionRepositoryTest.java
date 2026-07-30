package dev.yunseong.website.manage.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises the sessionisation SQL on the H2 test runtime. The 30-minute split is
 * expressed in window functions, so the query itself is the unit under test —
 * there is no Java implementation of it to test in isolation.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.hbm2ddl.auto=create-drop"
})
class VisitorSessionRepositoryTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 1, 10, 9, 0);
    private static final LocalDateTime PERIOD_START = BASE.minusDays(1);

    private static final String HUMAN_AGENT = "Mozilla/5.0 (Macintosh) Safari/605.1.15";
    private static final String OTHER_AGENT = "Mozilla/5.0 (Windows NT 10.0) Firefox/121.0";

    @Autowired
    private VisitorSessionRepository visitorSessionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void sessionsSplitAfterThirtyMinutesOfInactivity() {
        record_("1.1.1.1", HUMAN_AGENT, "/", BASE);
        record_("1.1.1.1", HUMAN_AGENT, "/public/memos/1", BASE.plusMinutes(30).plusSeconds(1));

        Summary summary = summarise();

        assertEquals(1, summary.visitors());
        assertEquals(2, summary.sessions());
        assertEquals(2, summary.pageViews());
    }

    @Test
    void requestExactlyThirtyMinutesLaterStaysInSameSession() {
        record_("1.1.1.1", HUMAN_AGENT, "/", BASE);
        record_("1.1.1.1", HUMAN_AGENT, "/public/memos/1", BASE.plusMinutes(30));

        Summary summary = summarise();

        assertEquals(1, summary.visitors());
        assertEquals(1, summary.sessions());
        assertEquals(2, summary.pageViews());
        assertEquals(1800, summary.medianSessionSeconds());
    }

    @Test
    void oneVisitorProducesMultipleSessions() {
        record_("1.1.1.1", HUMAN_AGENT, "/", BASE);
        record_("1.1.1.1", HUMAN_AGENT, "/public/memos/1", BASE.plusMinutes(5));
        record_("1.1.1.1", HUMAN_AGENT, "/public/memos/2", BASE.plusMinutes(10));
        record_("1.1.1.1", HUMAN_AGENT, "/", BASE.plusHours(2));
        record_("1.1.1.1", HUMAN_AGENT, "/public/memos/3", BASE.plusHours(5));

        Summary summary = summarise();

        assertEquals(1, summary.visitors());
        assertEquals(3, summary.sessions());
        assertEquals(5, summary.pageViews());
        // sessions of 3, 1 and 1 page views -> two single-page sessions
        assertEquals(2, summary.singlePageSessions());
        // one visitor with more than one session
        assertEquals(1, summary.returningVisitors());
    }

    @Test
    void visitorsAtTheSameInstantStaySeparate() {
        record_("1.1.1.1", HUMAN_AGENT, "/", BASE);
        record_("2.2.2.2", HUMAN_AGENT, "/", BASE);
        record_("1.1.1.1", OTHER_AGENT, "/", BASE);

        Summary summary = summarise();

        assertEquals(3, summary.visitors());
        assertEquals(3, summary.sessions());
        assertEquals(3, summary.singlePageSessions());
        assertEquals(0, summary.returningVisitors());
        assertEquals(0, summary.medianSessionSeconds());
    }

    @Test
    void botRequestsAreExcludedUnlessRequested() {
        record_("1.1.1.1", HUMAN_AGENT, "/", BASE);
        recordBot("9.9.9.9", "Googlebot/2.1", "/", BASE);

        assertEquals(1, summarise().visitors());
        assertEquals(2, summarise(true).visitors());
    }

    @Test
    void entryAndExitPagesAreCountedPerSession() {
        // session one: / -> /public/memos/1 -> /public/memos/2
        record_("1.1.1.1", HUMAN_AGENT, "/", BASE);
        record_("1.1.1.1", HUMAN_AGENT, "/public/memos/1", BASE.plusMinutes(1));
        record_("1.1.1.1", HUMAN_AGENT, "/public/memos/2", BASE.plusMinutes(2));
        // session two, after a gap: /public/memos/1 -> /
        record_("1.1.1.1", HUMAN_AGENT, "/public/memos/1", BASE.plusHours(3));
        record_("1.1.1.1", HUMAN_AGENT, "/", BASE.plusHours(3).plusMinutes(1));

        Map<String, List<Long>> counts = visitorSessionRepository
                .countEntryAndExitPages(PERIOD_START, false).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> List.of(((Number) row[1]).longValue(), ((Number) row[2]).longValue())));

        assertEquals(List.of(1L, 1L), counts.get("/"));
        assertEquals(List.of(1L, 0L), counts.get("/public/memos/1"));
        assertEquals(List.of(0L, 1L), counts.get("/public/memos/2"));
    }

    @Test
    void emptyPeriodReturnsZeroedSummary() {
        Summary summary = summarise();

        assertEquals(0, summary.sessions());
        assertEquals(0, summary.visitors());
        assertEquals(0, summary.pageViews());
    }

    private Summary summarise() {
        return summarise(false);
    }

    private Summary summarise(boolean includeBots) {
        Object[] row = visitorSessionRepository.summariseSessions(PERIOD_START, includeBots).get(0);
        return new Summary(
                asLong(row[0]), asLong(row[1]), asLong(row[2]),
                asLong(row[3]), asLong(row[4]),
                row[5] == null ? 0L : ((Number) row[5]).longValue());
    }

    private static long asLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private void record_(String ip, String userAgent, String uri, LocalDateTime at) {
        persist(ip, userAgent, uri, at, false);
    }

    private void recordBot(String ip, String userAgent, String uri, LocalDateTime at) {
        persist(ip, userAgent, uri, at, true);
    }

    /**
     * Inserted with native SQL because {@code @CreationTimestamp} would overwrite
     * the timestamps these tests depend on.
     */
    private void persist(String ip, String userAgent, String uri, LocalDateTime at, boolean isBot) {
        entityManager.createNativeQuery("""
                        INSERT INTO request_statistics (uri, method, user_agent, ip, status_code, created_at, is_bot)
                        VALUES (:uri, 'GET', :userAgent, :ip, 200, :createdAt, :isBot)
                        """)
                .setParameter("uri", uri)
                .setParameter("userAgent", userAgent)
                .setParameter("ip", ip)
                .setParameter("createdAt", at)
                .setParameter("isBot", isBot)
                .executeUpdate();
    }

    private record Summary(long sessions, long visitors, long returningVisitors,
                           long pageViews, long singlePageSessions, long medianSessionSeconds) {
    }
}
