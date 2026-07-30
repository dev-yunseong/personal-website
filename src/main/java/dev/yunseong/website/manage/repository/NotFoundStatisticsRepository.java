package dev.yunseong.website.manage.repository;

import dev.yunseong.website.manage.domain.NotFoundStat;
import dev.yunseong.website.manage.domain.RequestStatistics;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Read-only 404 aggregation. Grouping and the referer classification happen in
 * SQL so the report never loads the request rows themselves.
 *
 * <p>The two report queries differ only in ORDER BY, so they share the
 * projection through a compile-time constant.
 */
@Repository
public interface NotFoundStatisticsRepository extends JpaRepository<RequestStatistics, Long> {

    String NOT_FOUND_REPORT = """
            SELECT new dev.yunseong.website.manage.domain.NotFoundStat(
                       r.uri,
                       COUNT(r),
                       MIN(r.createdAt),
                       MAX(r.createdAt),
                       COALESCE(MAX(CASE WHEN r.referer LIKE :internalRefererPattern THEN r.referer END),
                                MAX(r.referer)),
                       COUNT(CASE WHEN r.referer LIKE :internalRefererPattern THEN 1 END),
                       COUNT(CASE WHEN r.referer IS NOT NULL AND r.referer NOT LIKE :internalRefererPattern THEN 1 END),
                       COUNT(CASE WHEN r.referer IS NULL THEN 1 END))
            FROM RequestStatistics r
            WHERE r.statusCode = 404 AND r.createdAt >= :startDate AND r.isBot IN :botFlags
            GROUP BY r.uri
            """;

    @Query(NOT_FOUND_REPORT + " ORDER BY COUNT(r) DESC, MAX(r.createdAt) DESC")
    List<NotFoundStat> findNotFoundStatsByOccurrences(@Param("startDate") LocalDateTime startDate,
                                                      @Param("botFlags") Collection<Boolean> botFlags,
                                                      @Param("internalRefererPattern") String internalRefererPattern,
                                                      Pageable pageable);

    @Query(NOT_FOUND_REPORT + " ORDER BY MAX(r.createdAt) DESC")
    List<NotFoundStat> findNotFoundStatsByLastSeen(@Param("startDate") LocalDateTime startDate,
                                                   @Param("botFlags") Collection<Boolean> botFlags,
                                                   @Param("internalRefererPattern") String internalRefererPattern,
                                                   Pageable pageable);

    @Query("""
            SELECT COUNT(DISTINCT r.uri) FROM RequestStatistics r
            WHERE r.statusCode = 404 AND r.createdAt >= :startDate AND r.isBot IN :botFlags
            """)
    long countNotFoundUris(@Param("startDate") LocalDateTime startDate,
                           @Param("botFlags") Collection<Boolean> botFlags);
}
