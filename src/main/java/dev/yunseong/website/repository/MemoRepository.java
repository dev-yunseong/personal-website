package dev.yunseong.website.repository;

import dev.yunseong.website.domain.Memo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MemoRepository extends JpaRepository<Memo, Long> {
    Optional<Memo> findByName(String name);

    @Query(
            value = """
        SELECT *
        FROM memos
        WHERE name ~ CONCAT('^', :path, '[^/]+$')
        ORDER BY created_at DESC
        """,
            countQuery = """
        SELECT count(*)
        FROM memos
        WHERE name ~ CONCAT('^', :path, '[^/]+$')
        """,
            nativeQuery = true
    )
    Page<Memo> findAllByPath(String path, Pageable pageable);
    Page<Memo> findAllByNameStartingWith(String name, Pageable pageable);
}
