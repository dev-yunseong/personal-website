package dev.yunseong.website.blog.repository;

import dev.yunseong.website.blog.domain.Memo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemoRepository extends JpaRepository<Memo, Long> {
    Optional<Memo> findByName(String name);

    @Query(
            value = """
        SELECT *
        FROM memos
        WHERE id = :id
          AND name NOT LIKE '/private%'
        """,
            nativeQuery = true
    )
    Optional<Memo> findPublicById(Long id);

    @Query(
            value = """
        SELECT *
        FROM memos
        WHERE name LIKE CONCAT('%', :query, '%')
        ORDER BY updated_at DESC
        """,
            countQuery = """
        SELECT count(*)
        FROM memos
        WHERE name LIKE CONCAT('%', :query, '%')
        """,
            nativeQuery = true
    )
    Page<Memo> findByQuery(String query, Pageable pageable);

    @Query(
            value = """
        SELECT *
        FROM memos
        WHERE name LIKE CONCAT('%', :query, '%')
          AND name NOT LIKE '/private%'
        ORDER BY updated_at DESC
        """,
            countQuery = """
        SELECT count(*)
        FROM memos
        WHERE name LIKE CONCAT('%', :query, '%')
          AND name NOT LIKE '/private%'
        """,
            nativeQuery = true
    )
    Page<Memo> findPublicByQuery(String query, Pageable pageable);

    @Query(
            value = """
        SELECT *
        FROM memos
        WHERE name ~ CONCAT('^', :path, '[^/]+$')
           OR name = RTRIM(:path, '/')
        ORDER BY updated_at DESC
        """,
            countQuery = """
        SELECT count(*)
        FROM memos
        WHERE name ~ CONCAT('^', :path, '[^/]+$')
           OR name = RTRIM(:path, '/')
        """,
            nativeQuery = true
    )
    Page<Memo> findAllByPath(String path, Pageable pageable);

    @Query(
            value = """
        SELECT *
        FROM memos
        WHERE (name ~ CONCAT('^', :path, '[^/]+$')
           OR name = RTRIM(:path, '/'))
          AND name NOT LIKE '/private%'
        ORDER BY updated_at DESC
        """,
            countQuery = """
        SELECT count(*)
        FROM memos
        WHERE (name ~ CONCAT('^', :path, '[^/]+$')
           OR name = RTRIM(:path, '/'))
          AND name NOT LIKE '/private%'
        """,
            nativeQuery = true
    )
    Page<Memo> findPublicByPath(String path, Pageable pageable);

    @Query("SELECT m FROM Memo m WHERE m.name NOT LIKE '/private%'")
    Page<Memo> findPublic(Pageable pageable);

    Page<Memo> findAllByNameStartingWith(String name, Pageable pageable);

    @Query("SELECT m FROM Memo m WHERE m.name LIKE CONCAT(:prefix, '%') AND m.name NOT LIKE CONCAT(:excludedPrefix, '%')")
    Page<Memo> findAllByNamePrefixExcludingPrefix(String prefix, String excludedPrefix, Pageable pageable);

    List<Memo> findAllByUpdatedAtGreaterThan(LocalDateTime time);
}
