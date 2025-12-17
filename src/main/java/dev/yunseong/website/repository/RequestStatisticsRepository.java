package dev.yunseong.website.repository;

import dev.yunseong.website.domain.RequestStatistics;
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

    @Query("SELECT r.uri, COUNT(r) as totalCount FROM RequestStatistics r WHERE r.createdAt >= :startDate GROUP BY r.uri ORDER BY totalCount DESC")
    List<Object[]> findTopUrisByRequestCount(@Param("startDate") LocalDateTime startDate);
}
