package dev.yunseong.website.manage.repository;

import dev.yunseong.website.manage.domain.RequestStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the request list's single filter query against a real database, so
 * the nullable-parameter JPQL behind {@code /admin/console/requests} is
 * verified rather than mocked away.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:requestfilter;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class RequestStatisticsRepositoryTest {

    private static final LocalDateTime WINDOW_START = LocalDateTime.now().minusDays(1);
    private static final PageRequest FIRST_PAGE = PageRequest.of(0, 10);

    @Autowired
    private RequestStatisticsRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.saveAll(List.of(
                request("/public/memos", "1.1.1.1", "curl/8.0", "https://news.example", 200, "KR"),
                request("/public/memos", "2.2.2.2", "Mozilla/5.0", "https://news.example", 404, "US"),
                request("/public/memos/1", "1.1.1.1", "curl/8.0", null, 500, "KR"),
                request("/", "3.3.3.3", "Mozilla/5.0", null, 200, null),
                noStatus("/public/blog", "4.4.4.4")
        ));
    }

    private static RequestStatistics request(String uri, String ip, String userAgent, String referer,
                                             Integer statusCode, String countryCode) {
        return new RequestStatistics(uri, "GET", referer, userAgent, ip, statusCode, false, 7, countryCode);
    }

    /** A row written before status codes were recorded; it must survive an unfiltered list. */
    private static RequestStatistics noStatus(String uri, String ip) {
        return new RequestStatistics(uri, "GET", null, "Mozilla/5.0", ip);
    }

    private Page<RequestStatistics> find(Integer minStatus, Integer maxStatus, String uri, String ip,
                                         String userAgent, String referer, String countryCode) {
        return repository.findMatchingRequests(WINDOW_START, minStatus, maxStatus,
                uri, ip, userAgent, referer, countryCode, FIRST_PAGE);
    }

    @Test
    void findMatchingRequests_WithEveryFilterNull_ReturnsEveryRowInWindow() {
        Page<RequestStatistics> page = find(null, null, null, null, null, null, null);

        assertEquals(5, page.getTotalElements());
    }

    @Test
    void findMatchingRequests_FiltersUriExactly() {
        Page<RequestStatistics> page = find(null, null, "/public/memos", null, null, null, null);

        // Exact match only: /public/memos/1 is a different URI, not a prefix hit.
        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent().stream().allMatch(r -> r.getUri().equals("/public/memos")));
    }

    @Test
    void findMatchingRequests_FiltersIpExactly() {
        Page<RequestStatistics> page = find(null, null, null, "1.1.1.1", null, null, null);

        assertEquals(2, page.getTotalElements());
    }

    @Test
    void findMatchingRequests_FiltersUserAgentExactly() {
        Page<RequestStatistics> page = find(null, null, null, null, "curl/8.0", null, null);

        assertEquals(2, page.getTotalElements());
    }

    @Test
    void findMatchingRequests_FiltersRefererExactly() {
        Page<RequestStatistics> page = find(null, null, null, null, null, "https://news.example", null);

        assertEquals(2, page.getTotalElements());
    }

    @Test
    void findMatchingRequests_FiltersCountryCodeExactly() {
        Page<RequestStatistics> page = find(null, null, null, null, null, null, "KR");

        assertEquals(2, page.getTotalElements());
    }

    @Test
    void findMatchingRequests_CombinesFiltersWithAnd() {
        Page<RequestStatistics> page = find(null, null, "/public/memos", "1.1.1.1", "curl/8.0", null, "KR");

        assertEquals(1, page.getTotalElements());
        assertEquals(200, page.getContent().get(0).getStatusCode());
    }

    @Test
    void findMatchingRequests_WithStatusRange_KeepsOnlyThatRange() {
        Page<RequestStatistics> page = find(400, 499, null, null, null, null, null);

        assertEquals(1, page.getTotalElements());
        assertEquals(404, page.getContent().get(0).getStatusCode());
    }

    @Test
    void findMatchingRequests_WithNullStatusRange_KeepsRowsWithNoStatusCode() {
        Page<RequestStatistics> unfiltered = find(null, null, "/public/blog", null, null, null, null);
        Page<RequestStatistics> ranged = find(200, 599, "/public/blog", null, null, null, null);

        assertEquals(1, unfiltered.getTotalElements());
        assertEquals(0, ranged.getTotalElements());
    }

    @Test
    void findMatchingRequests_ExcludesRowsBeforeTheWindow() {
        Page<RequestStatistics> page = repository.findMatchingRequests(
                LocalDateTime.now().plusDays(1), null, null, null, null, null, null, null, FIRST_PAGE);

        assertEquals(0, page.getTotalElements());
    }

    @Test
    void findMatchingRequests_PagesWithACountThatMatchesTheFilter() {
        Page<RequestStatistics> page = repository.findMatchingRequests(
                WINDOW_START, null, null, "/public/memos", null, null, null, null, PageRequest.of(0, 1));

        assertEquals(2, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
        assertEquals(1, page.getContent().size());
    }
}
