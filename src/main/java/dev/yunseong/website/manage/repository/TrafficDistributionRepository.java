package dev.yunseong.website.manage.repository;

import dev.yunseong.website.manage.domain.RequestStatistics;
import dev.yunseong.website.manage.domain.UriStat;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only aggregations behind the dashboard distribution tab.
 *
 * <p>Both queries take {@code includeBots}: {@code false} narrows to human
 * traffic, {@code true} turns the flag predicate into a tautology, so one query
 * serves both states of the toggle instead of a filtered/unfiltered pair.
 */
public interface TrafficDistributionRepository extends Repository<RequestStatistics, Long> {

    /**
     * Request counts per hour. Grouping happens in SQL and returns at most
     * {@code 24 * days} rows, which the caller folds onto the day-of-week by hour
     * grid. Folding in Java keeps the day-of-week origin unambiguous, where
     * {@code extract(day of week)} differs per dialect.
     */
    @Query("SELECT trunc(r.createdAt, HOUR), COUNT(r) FROM RequestStatistics r "
            + "WHERE r.createdAt >= :startDate AND (r.isBot = false OR :includeBots = true) "
            + "GROUP BY trunc(r.createdAt, HOUR)")
    List<Object[]> countByHour(@Param("startDate") LocalDateTime startDate,
                               @Param("includeBots") boolean includeBots);

    /**
     * Request counts per distinct User-Agent, so parsing runs once per value
     * instead of once per row.
     */
    @Query("SELECT new dev.yunseong.website.manage.domain.UriStat(r.userAgent, COUNT(r)) FROM RequestStatistics r "
            + "WHERE r.createdAt >= :startDate AND (r.isBot = false OR :includeBots = true) "
            + "GROUP BY r.userAgent")
    List<UriStat> countByUserAgent(@Param("startDate") LocalDateTime startDate,
                                   @Param("includeBots") boolean includeBots);
}
