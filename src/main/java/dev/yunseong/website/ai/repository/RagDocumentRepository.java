package dev.yunseong.website.ai.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import dev.yunseong.website.ai.domain.RagDocument;
import dev.yunseong.website.ai.domain.ResourceType;

public interface RagDocumentRepository extends JpaRepository<RagDocument, Long> {

    List<RagDocument> findAllByResourceIdInAndResourceType(
            Collection<Long> resourceIds,
            ResourceType resourceType);

    void deleteAllByResourceIdInAndResourceType(
            Collection<Long> resourceIds,
            ResourceType resourceType);

    @Query("SELECT MAX(r.updatedAt) FROM RagDocument r")
    Optional<LocalDateTime> findLastUpdatedAt();
}
