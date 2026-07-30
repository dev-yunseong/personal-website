package dev.yunseong.website.manage.repository;

import dev.yunseong.website.manage.domain.UriStat;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Executes the two aggregation queries against the test database, because
 * {@code trunc(createdAt, HOUR)} and the bot-inclusion predicate only fail at
 * query build time.
 */
@DataJpaTest
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class TrafficDistributionRepositoryTest {

    private static final LocalDateTime MONDAY_09 = LocalDateTime.of(2024, 1, 1, 9, 0);

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TrafficDistributionRepository trafficDistributionRepository;

    @Test
    void countByHour_GroupsPerHourAndExcludesBotsUnlessIncluded() {
        insertRequest(MONDAY_09.plusMinutes(5), "Mozilla/5.0 human", false);
        insertRequest(MONDAY_09.plusMinutes(40), "Mozilla/5.0 human", false);
        insertRequest(MONDAY_09.plusHours(1), "Mozilla/5.0 human", false);
        insertRequest(MONDAY_09.plusMinutes(10), "Googlebot/2.1", true);

        List<Object[]> humanOnly = trafficDistributionRepository.countByHour(MONDAY_09.minusDays(1), false);
        assertEquals(2, humanOnly.size());
        assertEquals(MONDAY_09, humanOnly.get(0)[0]);
        assertEquals(2L, ((Number) humanOnly.get(0)[1]).longValue());
        assertEquals(1L, ((Number) humanOnly.get(1)[1]).longValue());

        List<Object[]> withBots = trafficDistributionRepository.countByHour(MONDAY_09.minusDays(1), true);
        assertEquals(4L, withBots.stream().mapToLong(row -> ((Number) row[1]).longValue()).sum());
    }

    @Test
    void countByUserAgent_GroupsPerDistinctUserAgentAndExcludesBotsUnlessIncluded() {
        insertRequest(MONDAY_09, "Mozilla/5.0 human", false);
        insertRequest(MONDAY_09, "Mozilla/5.0 human", false);
        insertRequest(MONDAY_09, "Mozilla/5.0 other", false);
        insertRequest(MONDAY_09, "Googlebot/2.1", true);

        List<UriStat> humanOnly = trafficDistributionRepository.countByUserAgent(MONDAY_09.minusDays(1), false);
        assertEquals(2, humanOnly.size());
        assertEquals(3L, humanOnly.stream().mapToLong(UriStat::requestCount).sum());

        List<UriStat> withBots = trafficDistributionRepository.countByUserAgent(MONDAY_09.minusDays(1), true);
        assertEquals(3, withBots.size());
        assertEquals(4L, withBots.stream().mapToLong(UriStat::requestCount).sum());
    }

    /**
     * Native insert because {@code @CreationTimestamp} would overwrite the
     * timestamps these assertions depend on.
     */
    private void insertRequest(LocalDateTime createdAt, String userAgent, boolean isBot) {
        entityManager.createNativeQuery(
                        "INSERT INTO request_statistics (uri, method, user_agent, created_at, is_bot) "
                                + "VALUES ('/', 'GET', ?1, ?2, ?3)")
                .setParameter(1, userAgent)
                .setParameter(2, createdAt)
                .setParameter(3, isBot)
                .executeUpdate();
    }
}
