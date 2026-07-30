package dev.yunseong.website.manage.repository;

import dev.yunseong.website.manage.domain.CountryStat;
import dev.yunseong.website.manage.domain.RequestStatistics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Geo aggregation over {@code request_statistics}, kept separate from
 * {@link RequestStatisticsRepository} so the country queries and the backfill
 * evolve without touching the shared read queries.
 */
@Repository
public interface RequestGeoStatisticsRepository extends JpaRepository<RequestStatistics, Long> {

    @Query(value = """
            SELECT new dev.yunseong.website.manage.domain.CountryStat(r.countryCode, COUNT(r))
            FROM RequestStatistics r
            WHERE r.createdAt >= :startDate AND r.countryCode IS NOT NULL
              AND (:includeBots = true OR r.isBot = false)
            GROUP BY r.countryCode
            ORDER BY COUNT(r) DESC, r.countryCode ASC
            """,
            countQuery = """
                    SELECT COUNT(DISTINCT r.countryCode) FROM RequestStatistics r
                    WHERE r.createdAt >= :startDate AND r.countryCode IS NOT NULL
                      AND (:includeBots = true OR r.isBot = false)
                    """)
    Page<CountryStat> findCountryCounts(@Param("startDate") LocalDateTime startDate,
                                       @Param("includeBots") boolean includeBots,
                                       Pageable pageable);

    /** Requests in the window whose IP could not be mapped to a country. */
    @Query("""
            SELECT COUNT(r) FROM RequestStatistics r
            WHERE r.createdAt >= :startDate AND r.countryCode IS NULL
              AND (:includeBots = true OR r.isBot = false)
            """)
    long countWithoutCountry(@Param("startDate") LocalDateTime startDate,
                             @Param("includeBots") boolean includeBots);

    /**
     * Backfill input, chunked by distinct IP. Deliberately not filtered on
     * {@code countryCode IS NULL}: the IP set must stay stable while the
     * backfill writes, or offset paging would skip rows.
     */
    @Query("SELECT DISTINCT r.ip FROM RequestStatistics r WHERE r.ip IS NOT NULL ORDER BY r.ip ASC")
    List<String> findDistinctIps(Pageable pageable);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RequestStatistics r SET r.countryCode = :countryCode WHERE r.ip = :ip AND r.countryCode IS NULL")
    int assignCountryCode(@Param("ip") String ip, @Param("countryCode") String countryCode);
}
