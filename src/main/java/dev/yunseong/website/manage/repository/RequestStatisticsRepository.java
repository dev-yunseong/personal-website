package dev.yunseong.website.manage.repository;

import dev.yunseong.website.manage.domain.RequestStatistics;
import dev.yunseong.website.manage.domain.UriStat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RequestStatisticsRepository extends JpaRepository<RequestStatistics, Long> {
    @Query("SELECT r FROM RequestStatistics r WHERE r.createdAt >= :startDate ORDER BY r.createdAt DESC")
    List<RequestStatistics> findByCreatedAtAfter(@Param("startDate") LocalDateTime startDate);

    @Query(value = "SELECT r FROM RequestStatistics r WHERE r.createdAt >= :startDate ORDER BY r.createdAt DESC",
            countQuery = "SELECT COUNT(r) FROM RequestStatistics r WHERE r.createdAt >= :startDate")
    Page<RequestStatistics> findByCreatedAtAfter(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    @Query("SELECT COUNT(r) FROM RequestStatistics r WHERE r.createdAt >= :startDate")
    long countByCreatedAtAfter(@Param("startDate") LocalDateTime startDate);

    @Query(value = "SELECT r FROM RequestStatistics r WHERE r.createdAt >= :startDate AND r.statusCode >= :minStatus AND r.statusCode <= :maxStatus ORDER BY r.createdAt DESC",
            countQuery = "SELECT COUNT(r) FROM RequestStatistics r WHERE r.createdAt >= :startDate AND r.statusCode >= :minStatus AND r.statusCode <= :maxStatus")
    Page<RequestStatistics> findByCreatedAtAfterAndStatusCodeBetween(@Param("startDate") LocalDateTime startDate,
                                                                     @Param("minStatus") int minStatus,
                                                                     @Param("maxStatus") int maxStatus,
                                                                     Pageable pageable);

    @Query("SELECT COUNT(r) FROM RequestStatistics r WHERE r.createdAt >= :startDate AND r.statusCode >= :minStatus AND r.statusCode <= :maxStatus")
    long countByCreatedAtAfterAndStatusCodeBetween(@Param("startDate") LocalDateTime startDate,
                                                   @Param("minStatus") int minStatus,
                                                   @Param("maxStatus") int maxStatus);

    @Query("SELECT r.uri, COUNT(r) as totalCount FROM RequestStatistics r WHERE r.createdAt >= :startDate GROUP BY r.uri ORDER BY totalCount DESC")
    List<Object[]> findTopUrisByRequestCount(@Param("startDate") LocalDateTime startDate);

    @Query(value = "SELECT new dev.yunseong.website.manage.domain.UriStat(r.uri, COUNT(r)) FROM RequestStatistics r WHERE r.createdAt >= :startDate GROUP BY r.uri ORDER BY COUNT(r) DESC",
            countQuery = "SELECT COUNT(DISTINCT r.uri) FROM RequestStatistics r WHERE r.createdAt >= :startDate")
    Page<UriStat> findTopUrisByRequestCount(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    @Query(value = "SELECT new dev.yunseong.website.manage.domain.UriStat(r.uri, COUNT(r)) FROM RequestStatistics r WHERE r.createdAt >= :startDate AND r.statusCode >= :minStatus AND r.statusCode <= :maxStatus GROUP BY r.uri ORDER BY COUNT(r) DESC",
            countQuery = "SELECT COUNT(DISTINCT r.uri) FROM RequestStatistics r WHERE r.createdAt >= :startDate AND r.statusCode >= :minStatus AND r.statusCode <= :maxStatus")
    Page<UriStat> findTopUrisByRequestCountAndStatusCodeBetween(@Param("startDate") LocalDateTime startDate,
                                                                @Param("minStatus") int minStatus,
                                                                @Param("maxStatus") int maxStatus,
                                                                Pageable pageable);

    @Query(value = "SELECT new dev.yunseong.website.manage.domain.UriStat(r.uri, COUNT(r)) FROM RequestStatistics r WHERE r.createdAt >= :startDate GROUP BY r.uri ORDER BY COUNT(r) ASC",
            countQuery = "SELECT COUNT(DISTINCT r.uri) FROM RequestStatistics r WHERE r.createdAt >= :startDate")
    Page<UriStat> findTopUrisByCountAsc(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    @Query(value = "SELECT new dev.yunseong.website.manage.domain.UriStat(r.uri, COUNT(r)) FROM RequestStatistics r WHERE r.createdAt >= :startDate AND r.statusCode >= :minStatus AND r.statusCode <= :maxStatus GROUP BY r.uri ORDER BY COUNT(r) ASC",
            countQuery = "SELECT COUNT(DISTINCT r.uri) FROM RequestStatistics r WHERE r.createdAt >= :startDate AND r.statusCode >= :minStatus AND r.statusCode <= :maxStatus")
    Page<UriStat> findTopUrisByCountAscAndStatusCodeBetween(@Param("startDate") LocalDateTime startDate,
                                                            @Param("minStatus") int minStatus,
                                                            @Param("maxStatus") int maxStatus,
                                                            Pageable pageable);

    @Query(value = "SELECT new dev.yunseong.website.manage.domain.UriStat(r.uri, COUNT(r)) FROM RequestStatistics r WHERE r.createdAt >= :startDate GROUP BY r.uri",
            countQuery = "SELECT COUNT(DISTINCT r.uri) FROM RequestStatistics r WHERE r.createdAt >= :startDate")
    Page<UriStat> findTopUrisSortedByUri(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    @Query(value = "SELECT new dev.yunseong.website.manage.domain.UriStat(r.uri, COUNT(r)) FROM RequestStatistics r WHERE r.createdAt >= :startDate AND r.statusCode >= :minStatus AND r.statusCode <= :maxStatus GROUP BY r.uri",
            countQuery = "SELECT COUNT(DISTINCT r.uri) FROM RequestStatistics r WHERE r.createdAt >= :startDate AND r.statusCode >= :minStatus AND r.statusCode <= :maxStatus")
    Page<UriStat> findTopUrisSortedByUriAndStatusCodeBetween(@Param("startDate") LocalDateTime startDate,
                                                             @Param("minStatus") int minStatus,
                                                             @Param("maxStatus") int maxStatus,
                                                             Pageable pageable);

    // --- Group by IP ---

    @Query(value = "SELECT new dev.yunseong.website.manage.domain.UriStat(r.ip, COUNT(r)) FROM RequestStatistics r WHERE r.createdAt >= :startDate GROUP BY r.ip ORDER BY COUNT(r) DESC",
            countQuery = "SELECT COUNT(DISTINCT r.ip) FROM RequestStatistics r WHERE r.createdAt >= :startDate")
    Page<UriStat> findTopIpsByRequestCount(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    @Query(value = "SELECT new dev.yunseong.website.manage.domain.UriStat(r.ip, COUNT(r)) FROM RequestStatistics r WHERE r.createdAt >= :startDate AND r.statusCode >= :minStatus AND r.statusCode <= :maxStatus GROUP BY r.ip ORDER BY COUNT(r) DESC",
            countQuery = "SELECT COUNT(DISTINCT r.ip) FROM RequestStatistics r WHERE r.createdAt >= :startDate AND r.statusCode >= :minStatus AND r.statusCode <= :maxStatus")
    Page<UriStat> findTopIpsByRequestCountAndStatusCodeBetween(@Param("startDate") LocalDateTime startDate,
                                                               @Param("minStatus") int minStatus,
                                                               @Param("maxStatus") int maxStatus,
                                                               Pageable pageable);

    @Query(value = "SELECT new dev.yunseong.website.manage.domain.UriStat(r.ip, COUNT(r)) FROM RequestStatistics r WHERE r.createdAt >= :startDate GROUP BY r.ip ORDER BY COUNT(r) ASC",
            countQuery = "SELECT COUNT(DISTINCT r.ip) FROM RequestStatistics r WHERE r.createdAt >= :startDate")
    Page<UriStat> findTopIpsByCountAsc(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    @Query(value = "SELECT new dev.yunseong.website.manage.domain.UriStat(r.ip, COUNT(r)) FROM RequestStatistics r WHERE r.createdAt >= :startDate AND r.statusCode >= :minStatus AND r.statusCode <= :maxStatus GROUP BY r.ip ORDER BY COUNT(r) ASC",
            countQuery = "SELECT COUNT(DISTINCT r.ip) FROM RequestStatistics r WHERE r.createdAt >= :startDate AND r.statusCode >= :minStatus AND r.statusCode <= :maxStatus")
    Page<UriStat> findTopIpsByCountAscAndStatusCodeBetween(@Param("startDate") LocalDateTime startDate,
                                                           @Param("minStatus") int minStatus,
                                                           @Param("maxStatus") int maxStatus,
                                                           Pageable pageable);

    @Query(value = "SELECT new dev.yunseong.website.manage.domain.UriStat(r.ip, COUNT(r)) FROM RequestStatistics r WHERE r.createdAt >= :startDate GROUP BY r.ip",
            countQuery = "SELECT COUNT(DISTINCT r.ip) FROM RequestStatistics r WHERE r.createdAt >= :startDate")
    Page<UriStat> findTopIpsSortedByKey(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    @Query(value = "SELECT new dev.yunseong.website.manage.domain.UriStat(r.ip, COUNT(r)) FROM RequestStatistics r WHERE r.createdAt >= :startDate AND r.statusCode >= :minStatus AND r.statusCode <= :maxStatus GROUP BY r.ip",
            countQuery = "SELECT COUNT(DISTINCT r.ip) FROM RequestStatistics r WHERE r.createdAt >= :startDate AND r.statusCode >= :minStatus AND r.statusCode <= :maxStatus")
    Page<UriStat> findTopIpsSortedByKeyAndStatusCodeBetween(@Param("startDate") LocalDateTime startDate,
                                                            @Param("minStatus") int minStatus,
                                                            @Param("maxStatus") int maxStatus,
                                                            Pageable pageable);

    // --- Group by User Agent ---

    @Query(value = "SELECT new dev.yunseong.website.manage.domain.UriStat(r.userAgent, COUNT(r)) FROM RequestStatistics r WHERE r.createdAt >= :startDate GROUP BY r.userAgent ORDER BY COUNT(r) DESC",
            countQuery = "SELECT COUNT(DISTINCT r.userAgent) FROM RequestStatistics r WHERE r.createdAt >= :startDate")
    Page<UriStat> findTopUserAgentsByRequestCount(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    @Query(value = "SELECT new dev.yunseong.website.manage.domain.UriStat(r.userAgent, COUNT(r)) FROM RequestStatistics r WHERE r.createdAt >= :startDate AND r.statusCode >= :minStatus AND r.statusCode <= :maxStatus GROUP BY r.userAgent ORDER BY COUNT(r) DESC",
            countQuery = "SELECT COUNT(DISTINCT r.userAgent) FROM RequestStatistics r WHERE r.createdAt >= :startDate AND r.statusCode >= :minStatus AND r.statusCode <= :maxStatus")
    Page<UriStat> findTopUserAgentsByRequestCountAndStatusCodeBetween(@Param("startDate") LocalDateTime startDate,
                                                                      @Param("minStatus") int minStatus,
                                                                      @Param("maxStatus") int maxStatus,
                                                                      Pageable pageable);

    @Query(value = "SELECT new dev.yunseong.website.manage.domain.UriStat(r.userAgent, COUNT(r)) FROM RequestStatistics r WHERE r.createdAt >= :startDate GROUP BY r.userAgent ORDER BY COUNT(r) ASC",
            countQuery = "SELECT COUNT(DISTINCT r.userAgent) FROM RequestStatistics r WHERE r.createdAt >= :startDate")
    Page<UriStat> findTopUserAgentsByCountAsc(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    @Query(value = "SELECT new dev.yunseong.website.manage.domain.UriStat(r.userAgent, COUNT(r)) FROM RequestStatistics r WHERE r.createdAt >= :startDate AND r.statusCode >= :minStatus AND r.statusCode <= :maxStatus GROUP BY r.userAgent ORDER BY COUNT(r) ASC",
            countQuery = "SELECT COUNT(DISTINCT r.userAgent) FROM RequestStatistics r WHERE r.createdAt >= :startDate AND r.statusCode >= :minStatus AND r.statusCode <= :maxStatus")
    Page<UriStat> findTopUserAgentsByCountAscAndStatusCodeBetween(@Param("startDate") LocalDateTime startDate,
                                                                  @Param("minStatus") int minStatus,
                                                                  @Param("maxStatus") int maxStatus,
                                                                  Pageable pageable);

    @Query(value = "SELECT new dev.yunseong.website.manage.domain.UriStat(r.userAgent, COUNT(r)) FROM RequestStatistics r WHERE r.createdAt >= :startDate GROUP BY r.userAgent",
            countQuery = "SELECT COUNT(DISTINCT r.userAgent) FROM RequestStatistics r WHERE r.createdAt >= :startDate")
    Page<UriStat> findTopUserAgentsSortedByKey(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    @Query(value = "SELECT new dev.yunseong.website.manage.domain.UriStat(r.userAgent, COUNT(r)) FROM RequestStatistics r WHERE r.createdAt >= :startDate AND r.statusCode >= :minStatus AND r.statusCode <= :maxStatus GROUP BY r.userAgent",
            countQuery = "SELECT COUNT(DISTINCT r.userAgent) FROM RequestStatistics r WHERE r.createdAt >= :startDate AND r.statusCode >= :minStatus AND r.statusCode <= :maxStatus")
    Page<UriStat> findTopUserAgentsSortedByKeyAndStatusCodeBetween(@Param("startDate") LocalDateTime startDate,
                                                                   @Param("minStatus") int minStatus,
                                                                   @Param("maxStatus") int maxStatus,
                                                                   Pageable pageable);

    // --- Timeline (daily request counts) ---

    @Query("SELECT CAST(r.createdAt AS date), COUNT(r) FROM RequestStatistics r WHERE r.createdAt >= :startDate GROUP BY CAST(r.createdAt AS date) ORDER BY CAST(r.createdAt AS date) ASC")
    List<Object[]> findDailyRequestCounts(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT CAST(r.createdAt AS date), COUNT(r) FROM RequestStatistics r WHERE r.createdAt >= :startDate AND r.statusCode >= :minStatus AND r.statusCode <= :maxStatus GROUP BY CAST(r.createdAt AS date) ORDER BY CAST(r.createdAt AS date) ASC")
    List<Object[]> findDailyRequestCountsAndStatusCodeBetween(@Param("startDate") LocalDateTime startDate,
                                                              @Param("minStatus") int minStatus,
                                                              @Param("maxStatus") int maxStatus);

    // --- Timeline (hourly request counts) ---

    @Query("SELECT trunc(r.createdAt, HOUR), COUNT(r) FROM RequestStatistics r WHERE r.createdAt >= :startDate GROUP BY trunc(r.createdAt, HOUR) ORDER BY trunc(r.createdAt, HOUR) ASC")
    List<Object[]> findHourlyRequestCounts(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT trunc(r.createdAt, HOUR), COUNT(r) FROM RequestStatistics r WHERE r.createdAt >= :startDate AND r.statusCode >= :minStatus AND r.statusCode <= :maxStatus GROUP BY trunc(r.createdAt, HOUR) ORDER BY trunc(r.createdAt, HOUR) ASC")
    List<Object[]> findHourlyRequestCountsAndStatusCodeBetween(@Param("startDate") LocalDateTime startDate,
                                                               @Param("minStatus") int minStatus,
                                                               @Param("maxStatus") int maxStatus);

    // --- Timeline (monthly request counts) ---

    @Query("SELECT trunc(r.createdAt, MONTH), COUNT(r) FROM RequestStatistics r WHERE r.createdAt >= :startDate GROUP BY trunc(r.createdAt, MONTH) ORDER BY trunc(r.createdAt, MONTH) ASC")
    List<Object[]> findMonthlyRequestCounts(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT trunc(r.createdAt, MONTH), COUNT(r) FROM RequestStatistics r WHERE r.createdAt >= :startDate AND r.statusCode >= :minStatus AND r.statusCode <= :maxStatus GROUP BY trunc(r.createdAt, MONTH) ORDER BY trunc(r.createdAt, MONTH) ASC")
    List<Object[]> findMonthlyRequestCountsAndStatusCodeBetween(@Param("startDate") LocalDateTime startDate,
                                                                @Param("minStatus") int minStatus,
                                                                @Param("maxStatus") int maxStatus);
}
