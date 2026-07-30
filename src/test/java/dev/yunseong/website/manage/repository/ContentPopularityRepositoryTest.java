package dev.yunseong.website.manage.repository;

import dev.yunseong.website.manage.domain.RequestStatistics;
import dev.yunseong.website.manage.domain.UriStat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:content-popularity;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
class ContentPopularityRepositoryTest {

    @Autowired
    private ContentPopularityRepository contentPopularityRepository;

    @BeforeEach
    void seed() {
        contentPopularityRepository.saveAll(List.of(
                requestTo("/public/memos/42", false),
                requestTo("/public/memos/42", false),
                requestTo("/public/memos/42?page=1", false),
                requestTo("/public/memos/9", true),
                requestTo("/public/memos", false),
                requestTo("/public/memos?category=/dev", false),
                requestTo("/", false)
        ));
    }

    private static RequestStatistics requestTo(String uri, boolean isBot) {
        return new RequestStatistics(uri, "GET", null, "agent", "1.1.1.1", 200, isBot, 5);
    }

    private Map<String, Long> countsFor(boolean includeBots) {
        List<UriStat> stats = contentPopularityRepository.countRequestsPerMemoUri(
                LocalDateTime.now().minusDays(1), includeBots);
        return stats.stream().collect(Collectors.toMap(UriStat::uri, UriStat::requestCount));
    }

    @Test
    void countRequestsPerMemoUri_ExcludingBots_GroupsOnlyHumanMemoDetailTraffic() {
        assertEquals(Map.of("/public/memos/42", 2L, "/public/memos/42?page=1", 1L), countsFor(false));
    }

    @Test
    void countRequestsPerMemoUri_IncludingBots_AlsoCountsBotTraffic() {
        assertEquals(Map.of("/public/memos/42", 2L, "/public/memos/42?page=1", 1L, "/public/memos/9", 1L),
                countsFor(true));
    }

    @Test
    void countRequestsPerMemoUri_OutsidePeriod_ReturnsNothing() {
        assertTrue(contentPopularityRepository.countRequestsPerMemoUri(
                LocalDateTime.now().plusDays(1), true).isEmpty());
    }

}
