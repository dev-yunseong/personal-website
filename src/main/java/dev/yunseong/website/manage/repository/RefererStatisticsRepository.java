package dev.yunseong.website.manage.repository;

import dev.yunseong.website.manage.domain.RefererGroupCount;
import dev.yunseong.website.manage.domain.RequestStatistics;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only referer aggregation. Kept apart from {@code RequestStatisticsRepository}
 * because traffic-source reporting needs exactly one query.
 */
public interface RefererStatisticsRepository extends Repository<RequestStatistics, Long> {

    /**
     * Counts requests per distinct referer, split by bot flag so both states of the
     * bot toggle are served by a single query. NULL referers group together and
     * become the direct-traffic bucket. The result is bounded by distinct referer
     * count, never by row count.
     */
    @Query("""
            SELECT new dev.yunseong.website.manage.domain.RefererGroupCount(r.referer, r.isBot, COUNT(r))
            FROM RequestStatistics r
            WHERE r.createdAt >= :startDate
            GROUP BY r.referer, r.isBot
            """)
    List<RefererGroupCount> countByRefererAndBot(@Param("startDate") LocalDateTime startDate);
}
