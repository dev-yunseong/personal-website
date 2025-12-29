package dev.yunseong.website.ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import dev.yunseong.website.ai.domain.RagDocument;

public interface RagDocumentRepository extends JpaRepository<RagDocument, Long> {

}
