package dev.yunseong.website.manage.repository;

import dev.yunseong.website.manage.domain.RefererGroupCount;
import dev.yunseong.website.manage.domain.RequestStatistics;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises the referer aggregation against a real database, since the grouping
 * and the {@code isBot} attribute path are only verified by running the query.
 */
@DataJpaTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never"
})
class RefererStatisticsRepositoryTest {

    @Autowired
    private RefererStatisticsRepository refererStatisticsRepository;

    @Autowired
    private EntityManager entityManager;

    private void persist(String referer, boolean bot) {
        entityManager.persist(new RequestStatistics("/", "GET", referer, "agent", "1.1.1.1", 200, bot, 5));
    }

    @Test
    void countByRefererAndBot_GroupsByRefererAndBotFlag() {
        persist("https://www.google.com/search?q=x", false);
        persist("https://www.google.com/search?q=x", false);
        persist("https://www.google.com/search?q=x", true);
        persist("https://news.ycombinator.com/item?id=1", false);
        persist(null, false);
        persist(null, true);
        persist(null, true);
        entityManager.flush();

        List<RefererGroupCount> groups =
                refererStatisticsRepository.countByRefererAndBot(LocalDateTime.now().minusDays(1));

        Map<String, Long> counts = groups.stream().collect(Collectors.toMap(
                group -> group.referer() + "|" + group.bot(), RefererGroupCount::requestCount));
        assertEquals(Map.of(
                "https://www.google.com/search?q=x|false", 2L,
                "https://www.google.com/search?q=x|true", 1L,
                "https://news.ycombinator.com/item?id=1|false", 1L,
                "null|false", 1L,
                "null|true", 2L), counts);
    }

    @Test
    void countByRefererAndBot_ExcludesRowsBeforeThePeriodStart() {
        persist("https://www.google.com/", false);
        entityManager.flush();

        List<RefererGroupCount> groups =
                refererStatisticsRepository.countByRefererAndBot(LocalDateTime.now().plusDays(1));

        assertEquals(List.of(), groups);
    }
}
