package dev.yunseong.website.manage.repository;

import dev.yunseong.website.manage.domain.NotFoundStat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the 404 aggregation against the test database, so the JPQL and the
 * referer classification are verified end to end rather than mocked away.
 */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "DATABASE_URL=jdbc:h2:mem:notfound",
        "DATABASE_USER=sa",
        "DATABASE_PW=",
        "APPLICATION_USER=admin",
        "APPLICATION_PW=admin"
})
class NotFoundStatisticsRepositoryTest {

    private static final String INTERNAL_PATTERN = "%//yunseong.dev/%";
    private static final List<Boolean> HUMANS = List.of(false);
    private static final List<Boolean> EVERYONE = List.of(false, true);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 30, 12, 0);
    private static final LocalDateTime START = NOW.minusDays(7);

    @Autowired
    private NotFoundStatisticsRepository notFoundStatisticsRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findNotFoundStatsByOccurrences_AggregatesRefererOriginsAndSeenTimes() {
        insertRequest("/public/memos/900", 404, "https://yunseong.dev/public/memos", false, NOW.minusDays(3));
        insertRequest("/public/memos/900", 404, "https://yunseong.dev/", false, NOW.minusDays(1));
        insertRequest("/public/memos/900", 404, "https://news.example.com/post", false, NOW.minusDays(2));
        insertRequest("/public/memos/900", 404, null, false, NOW.minusHours(5));
        insertRequest("/public/memos/901", 404, null, false, NOW.minusHours(2));
        insertRequest("/public/memos/902", 200, null, false, NOW.minusHours(2));
        insertRequest("/public/memos/903", 404, null, false, NOW.minusDays(30));

        List<NotFoundStat> stats = notFoundStatisticsRepository.findNotFoundStatsByOccurrences(
                START, HUMANS, INTERNAL_PATTERN, PageRequest.of(0, 20));

        assertEquals(List.of("/public/memos/900", "/public/memos/901"),
                stats.stream().map(NotFoundStat::uri).toList());
        NotFoundStat top = stats.get(0);
        assertEquals(4L, top.occurrences());
        assertEquals(NOW.minusDays(3), top.firstSeen());
        assertEquals(NOW.minusHours(5), top.lastSeen());
        assertEquals(2L, top.internalRefererCount());
        assertEquals(1L, top.externalRefererCount());
        assertEquals(1L, top.directCount());
        assertTrue(top.sampleReferer().startsWith("https://yunseong.dev"),
                "an internal referer is the most actionable representative, was " + top.sampleReferer());
    }

    @Test
    void findNotFoundStatsByLastSeen_OrdersMostRecentFirst() {
        insertRequest("/public/older", 404, null, false, NOW.minusDays(4));
        insertRequest("/public/older", 404, null, false, NOW.minusDays(3));
        insertRequest("/public/newer", 404, null, false, NOW.minusHours(1));

        List<NotFoundStat> stats = notFoundStatisticsRepository.findNotFoundStatsByLastSeen(
                START, HUMANS, INTERNAL_PATTERN, PageRequest.of(0, 20));

        assertEquals(List.of("/public/newer", "/public/older"),
                stats.stream().map(NotFoundStat::uri).toList());
    }

    @Test
    void notFoundQueries_ExcludeBotsUnlessRequested() {
        insertRequest("/public/human", 404, null, false, NOW.minusHours(1));
        insertRequest("/public/bot", 404, null, true, NOW.minusHours(1));

        List<NotFoundStat> humansOnly = notFoundStatisticsRepository.findNotFoundStatsByOccurrences(
                START, HUMANS, INTERNAL_PATTERN, PageRequest.of(0, 20));
        List<NotFoundStat> everyone = notFoundStatisticsRepository.findNotFoundStatsByOccurrences(
                START, EVERYONE, INTERNAL_PATTERN, PageRequest.of(0, 20));

        assertEquals(List.of("/public/human"), humansOnly.stream().map(NotFoundStat::uri).toList());
        assertEquals(2, everyone.size());
        assertEquals(1L, notFoundStatisticsRepository.countNotFoundUris(START, HUMANS));
        assertEquals(2L, notFoundStatisticsRepository.countNotFoundUris(START, EVERYONE));
    }

    @Test
    void findNotFoundStatsByOccurrences_AppliesPageableAsPagination() {
        insertRequest("/public/a", 404, null, false, NOW.minusHours(3));
        insertRequest("/public/a", 404, null, false, NOW.minusHours(2));
        insertRequest("/public/b", 404, null, false, NOW.minusHours(1));

        List<NotFoundStat> firstPage = notFoundStatisticsRepository.findNotFoundStatsByOccurrences(
                START, HUMANS, INTERNAL_PATTERN, PageRequest.of(0, 1));
        List<NotFoundStat> secondPage = notFoundStatisticsRepository.findNotFoundStatsByOccurrences(
                START, HUMANS, INTERNAL_PATTERN, PageRequest.of(1, 1));

        assertEquals(List.of("/public/a"), firstPage.stream().map(NotFoundStat::uri).toList());
        assertEquals(List.of("/public/b"), secondPage.stream().map(NotFoundStat::uri).toList());
    }

    /**
     * Inserted natively because {@code @CreationTimestamp} would overwrite the
     * timestamps this report is built on.
     */
    private void insertRequest(String uri, int statusCode, String referer, boolean isBot, LocalDateTime createdAt) {
        entityManager.getEntityManager()
                .createNativeQuery("""
                        INSERT INTO request_statistics (uri, method, referer, user_agent, ip, status_code, is_bot, created_at)
                        VALUES (?1, 'GET', ?2, 'Mozilla/5.0', '1.1.1.1', ?3, ?4, ?5)
                        """)
                .setParameter(1, uri)
                .setParameter(2, referer)
                .setParameter(3, statusCode)
                .setParameter(4, isBot)
                .setParameter(5, createdAt)
                .executeUpdate();
    }
}
