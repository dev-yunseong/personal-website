package dev.yunseong.website.manage.repository;

import dev.yunseong.website.manage.domain.CountryStat;
import dev.yunseong.website.manage.domain.CityStat;
import dev.yunseong.website.manage.domain.GeoRequestStat;
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
 * Exercises the country aggregation and the backfill statements against a real
 * database, so the JPQL is verified rather than mocked away.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:geostats;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class RequestGeoStatisticsRepositoryTest {

    private static final LocalDateTime WINDOW_START = LocalDateTime.now().minusDays(1);

    @Autowired
    private RequestGeoStatisticsRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        repository.saveAll(List.of(
                request("1.1.1.1", false, "KR", 1835848L, "Seoul", 37.56, 126.98, 20),
                request("1.1.1.2", false, "KR", 1835848L, "Seoul", 37.58, 127.00, 50),
                request("2.2.2.2", true, "KR", 1835848L, "Seoul", 37.57, 126.99, 20),
                request("3.3.3.3", false, "US", 5128581L, "New York", 40.71, -74.01, 20),
                request("10.0.0.1", false, null)
        ));
    }

    private static RequestStatistics request(String ip, boolean isBot, String countryCode) {
        return new RequestStatistics("/", "GET", null, "Mozilla/5.0", ip, 200, isBot, 5, countryCode);
    }

    private static RequestStatistics request(
            String ip, boolean isBot, String countryCode, Long cityId, String cityName,
            Double latitude, Double longitude, Integer accuracyRadiusKm) {
        RequestStatistics request = request(ip, isBot, countryCode);
        request.setCityGeoNameId(cityId);
        request.setCityName(cityName);
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        request.setAccuracyRadiusKm(accuracyRadiusKm);
        return request;
    }

    @Test
    void findCountryCounts_ExcludesBotsAndUnresolvedRows() {
        Page<CountryStat> page = repository.findCountryCounts(WINDOW_START, false, PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
        assertEquals(new CountryStat("KR", 2L), page.getContent().get(0));
        assertEquals(new CountryStat("US", 1L), page.getContent().get(1));
    }

    @Test
    void findCountryCounts_WithIncludeBots_CountsBotTraffic() {
        Page<CountryStat> page = repository.findCountryCounts(WINDOW_START, true, PageRequest.of(0, 10));

        assertEquals(new CountryStat("KR", 3L), page.getContent().get(0));
    }

    @Test
    void findCountryCounts_PaginatesByRequestCount() {
        Page<CountryStat> page = repository.findCountryCounts(WINDOW_START, false, PageRequest.of(1, 1));

        assertEquals(2, page.getTotalPages());
        assertEquals(List.of(new CountryStat("US", 1L)), page.getContent());
    }

    @Test
    void countWithoutCountry_CountsUnresolvedRequests() {
        assertEquals(1L, repository.countWithoutCountry(WINDOW_START, false));
    }

    @Test
    void findDistinctIps_ChunksSortedIps() {
        assertEquals(List.of("1.1.1.1", "1.1.1.2"), repository.findDistinctIps(PageRequest.of(0, 2)));
        assertEquals(List.of("10.0.0.1", "2.2.2.2"), repository.findDistinctIps(PageRequest.of(1, 2)));
    }

    @Test
    void findCityCounts_GroupsByCityIdAndUsesDeterministicLocationSummary() {
        List<CityStat> cities = repository.findCityCounts(WINDOW_START, "KR", true);

        assertEquals(1, cities.size());
        CityStat seoul = cities.get(0);
        assertEquals(1835848L, seoul.cityId());
        assertEquals("Seoul", seoul.cityName());
        assertEquals(37.57, seoul.latitude(), 0.0001);
        assertEquals(126.99, seoul.longitude(), 0.0001);
        assertEquals(50, seoul.accuracyRadiusKm());
        assertEquals(3L, seoul.requestCount());
        assertEquals(1L, seoul.botRequestCount());
    }

    @Test
    void findGeoRequests_FiltersCityAndNeverProjectsIp() {
        Page<GeoRequestStat> page = repository.findGeoRequests(
                WINDOW_START, "KR", false, 1835848L, PageRequest.of(0, 20));

        assertEquals(2L, page.getTotalElements());
        assertEquals("Seoul", page.getContent().get(0).cityName());
        assertEquals("/", page.getContent().get(0).uri());
    }

    @Test
    void assignLocation_OnlyFillsMissingFields() {
        assertEquals(1, repository.assignLocation(
                "10.0.0.1", "JP", 1850147L, "Tokyo", 35.6895, 139.6917, 20));
        assertEquals(0, repository.assignLocation(
                "10.0.0.1", "JP", 1850147L, "Tokyo", 35.6895, 139.6917, 20));
        assertEquals(0, repository.assignLocation(
                "1.1.1.1", "JP", 1850147L, "Tokyo", 35.6895, 139.6917, 20));

        assertEquals(0L, repository.countWithoutCountry(WINDOW_START, false));
        assertTrue(repository.findCountryCounts(WINDOW_START, false, PageRequest.of(0, 10))
                .getContent().contains(new CountryStat("JP", 1L)));
        assertEquals(1, repository.findCityCounts(WINDOW_START, "JP", false).size());
    }
}
