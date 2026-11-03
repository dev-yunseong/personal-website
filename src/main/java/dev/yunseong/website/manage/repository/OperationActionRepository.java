package dev.yunseong.website.manage.repository;

import dev.yunseong.website.manage.domain.OperationAction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OperationActionRepository extends JpaRepository<OperationAction, String> {
    List<OperationAction> findTop20ByProjectSlugOrderByRequestedAtDesc(String projectSlug);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OperationAction> findFirstByProjectSlugAndStateOrderByRequestedAtAscIdAsc(String projectSlug, OperationAction.State state);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select action from OperationAction action where action.projectSlug = :projectSlug and action.state = 'RUNNING' and action.leaseExpiresAt < :now")
    List<OperationAction> findExpiredRunningForUpdate(@Param("projectSlug") String projectSlug, @Param("now") Instant now);
}
