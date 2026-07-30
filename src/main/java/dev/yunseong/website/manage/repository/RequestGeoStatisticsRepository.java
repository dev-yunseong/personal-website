package dev.yunseong.website.manage.repository;

import dev.yunseong.website.manage.domain.CountryStat;
import dev.yunseong.website.manage.domain.CityStat;
import dev.yunseong.website.manage.domain.GeoRequestStat;
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

    @Query("""
            SELECT new dev.yunseong.website.manage.domain.CityStat(
                r.cityGeoNameId, MAX(r.cityName), AVG(r.latitude), AVG(r.longitude),
                MAX(r.accuracyRadiusKm), COUNT(r),
                SUM(CASE WHEN r.isBot = true THEN 1L ELSE 0L END))
            FROM RequestStatistics r
            WHERE r.createdAt >= :startDate AND r.countryCode = :countryCode
              AND r.cityGeoNameId IS NOT NULL AND r.latitude IS NOT NULL AND r.longitude IS NOT NULL
              AND (:includeBots = true OR r.isBot = false)
            GROUP BY r.cityGeoNameId
            ORDER BY COUNT(r) DESC, r.cityGeoNameId ASC
            """)
    List<CityStat> findCityCounts(@Param("startDate") LocalDateTime startDate,
                                  @Param("countryCode") String countryCode,
                                  @Param("includeBots") boolean includeBots);

    @Query("""
            SELECT COUNT(r) FROM RequestStatistics r
            WHERE r.createdAt >= :startDate AND r.countryCode = :countryCode
              AND (r.cityGeoNameId IS NULL OR r.latitude IS NULL OR r.longitude IS NULL)
              AND (:includeBots = true OR r.isBot = false)
            """)
    long countWithoutCity(@Param("startDate") LocalDateTime startDate,
                          @Param("countryCode") String countryCode,
                          @Param("includeBots") boolean includeBots);

    @Query(value = """
            SELECT new dev.yunseong.website.manage.domain.GeoRequestStat(
                r.uri, r.statusCode, r.isBot, r.cityName, r.createdAt)
            FROM RequestStatistics r
            WHERE r.createdAt >= :startDate AND r.countryCode = :countryCode
              AND (:includeBots = true OR r.isBot = false)
              AND (:cityId IS NULL OR r.cityGeoNameId = :cityId)
            ORDER BY r.createdAt DESC, r.id DESC
            """,
            countQuery = """
                    SELECT COUNT(r) FROM RequestStatistics r
                    WHERE r.createdAt >= :startDate AND r.countryCode = :countryCode
                      AND (:includeBots = true OR r.isBot = false)
                      AND (:cityId IS NULL OR r.cityGeoNameId = :cityId)
                    """)
    Page<GeoRequestStat> findGeoRequests(@Param("startDate") LocalDateTime startDate,
                                         @Param("countryCode") String countryCode,
                                         @Param("includeBots") boolean includeBots,
                                         @Param("cityId") Long cityId,
                                         Pageable pageable);

    /**
     * Backfill input, chunked by distinct IP. Deliberately not filtered on
     * {@code countryCode IS NULL}: the IP set must stay stable while the
     * backfill writes, or offset paging would skip rows.
     */
    @Query("SELECT DISTINCT r.ip FROM RequestStatistics r WHERE r.ip IS NOT NULL ORDER BY r.ip ASC")
    List<String> findDistinctIps(Pageable pageable);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE RequestStatistics r SET
                r.countryCode = COALESCE(r.countryCode, :countryCode),
                r.cityGeoNameId = COALESCE(r.cityGeoNameId, :cityId),
                r.cityName = COALESCE(r.cityName, :cityName),
                r.latitude = COALESCE(r.latitude, :latitude),
                r.longitude = COALESCE(r.longitude, :longitude),
                r.accuracyRadiusKm = COALESCE(r.accuracyRadiusKm, :accuracyRadiusKm)
            WHERE r.ip = :ip AND (
                (r.countryCode IS NULL AND :countryCode IS NOT NULL)
                OR (r.cityGeoNameId IS NULL AND :cityId IS NOT NULL)
                OR (r.cityName IS NULL AND :cityName IS NOT NULL)
                OR (r.latitude IS NULL AND :latitude IS NOT NULL)
                OR (r.longitude IS NULL AND :longitude IS NOT NULL)
                OR (r.accuracyRadiusKm IS NULL AND :accuracyRadiusKm IS NOT NULL)
            )
            """)
    int assignLocation(@Param("ip") String ip,
                       @Param("countryCode") String countryCode,
                       @Param("cityId") Long cityId,
                       @Param("cityName") String cityName,
                       @Param("latitude") Double latitude,
                       @Param("longitude") Double longitude,
                       @Param("accuracyRadiusKm") Integer accuracyRadiusKm);
}
