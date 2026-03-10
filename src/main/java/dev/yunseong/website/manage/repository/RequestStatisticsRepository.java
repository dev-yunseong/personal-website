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

    @Query(value = "SELECT new dev.yunseong.website.manage.domain.UriStat(r.uri, COUNT(r)) FROM RequestStatistics r WHERE r.createdAt >= :startDate GROUP BY r.uri ORDER BY COUNT(r) ASC",
            countQuery = "SELECT COUNT(DISTINCT r.uri) FROM RequestStatistics r WHERE r.createdAt >= :startDate")
    Page<UriStat> findTopUrisByCountAsc(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    @Query(value = "SELECT new dev.yunseong.website.manage.domain.UriStat(r.uri, COUNT(r)) FROM RequestStatistics r WHERE r.createdAt >= :startDate GROUP BY r.uri",
            countQuery = "SELECT COUNT(DISTINCT r.uri) FROM RequestStatistics r WHERE r.createdAt >= :startDate")
    Page<UriStat> findTopUrisSortedByUri(@Param("startDate") LocalDateTime startDate, Pageable pageable);
}
