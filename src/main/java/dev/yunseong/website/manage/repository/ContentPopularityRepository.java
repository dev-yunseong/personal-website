package dev.yunseong.website.manage.repository;

import dev.yunseong.website.manage.domain.RequestStatistics;
import dev.yunseong.website.manage.domain.UriStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ContentPopularityRepository extends JpaRepository<RequestStatistics, Long> {

    /**
     * Counts requests per memo detail URI inside the period.
     *
     * <p>Grouping happens in the database; the application only receives one row
     * per distinct memo detail URI, which query-string and trailing-slash variants
     * of the same memo still split into several rows for the service to merge.
     */
    @Query("""
            SELECT new dev.yunseong.website.manage.domain.UriStat(r.uri, COUNT(r))
            FROM RequestStatistics r
            WHERE r.createdAt >= :startDate
              AND r.uri LIKE '/public/memos/%'
              AND (:includeBots = true OR r.isBot = false)
            GROUP BY r.uri
            """)
    List<UriStat> countRequestsPerMemoUri(@Param("startDate") LocalDateTime startDate,
                                         @Param("includeBots") boolean includeBots);
}
