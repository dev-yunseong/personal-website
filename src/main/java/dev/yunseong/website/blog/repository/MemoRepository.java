package dev.yunseong.website.blog.repository;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.domain.SitemapMemoDto;
import dev.yunseong.website.blog.domain.MemoNavigationEntry;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemoRepository extends JpaRepository<Memo, Long> {
    Optional<Memo> findByName(String name);

    @Query("SELECT new dev.yunseong.website.blog.domain.MemoNavigationEntry(m.id, m.name, m.updatedAt, false) "
            + "FROM Memo m WHERE m.name NOT LIKE '/private%' "
            + "AND (m.name = :root OR LOCATE(:prefix, m.name) = 1)")
    List<MemoNavigationEntry> findPublicNavigation(@Param("root") String root, @Param("prefix") String prefix);

    @Query("SELECT new dev.yunseong.website.blog.domain.MemoNavigationEntry(m.id, m.name, m.updatedAt, true) "
            + "FROM Memo m WHERE LOCATE('/private/draft/', m.name) = 1 "
            + "AND (m.name = :root OR LOCATE(:prefix, m.name) = 1)")
    List<MemoNavigationEntry> findDraftNavigation(@Param("root") String root, @Param("prefix") String prefix);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Memo m WHERE m.id = :id")
    Optional<Memo> findByIdForUpdate(@Param("id") long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Memo m WHERE m.name = :name")
    Optional<Memo> findByNameForUpdate(@Param("name") String name);

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

    /**
     * Id breaks {@code updatedAt} ties so paging is stable. Without it two memos
     * sharing a timestamp have no defined order between them, and a page
     * boundary falling between them can repeat one memo and skip the other.
     */
    @Query("SELECT m FROM Memo m WHERE m.name NOT LIKE '/private%' ORDER BY m.updatedAt DESC, m.id ASC")
    Page<Memo> findPublic(Pageable pageable);

    Page<Memo> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    @Query("SELECT m FROM Memo m WHERE m.name NOT LIKE '/private%' AND m.updatedAt > :updatedAfter "
            + "ORDER BY m.updatedAt DESC, m.id ASC")
    Page<Memo> findPublicUpdatedAfter(
            @Param("updatedAfter") LocalDateTime updatedAfter,
            Pageable pageable);

    Page<Memo> findAllByNameStartingWith(String name, Pageable pageable);

    @Query("SELECT m FROM Memo m WHERE m.name LIKE CONCAT(:prefix, '%') AND m.name NOT LIKE CONCAT(:excludedPrefix, '%')")
    Page<Memo> findAllByNamePrefixExcludingPrefix(String prefix, String excludedPrefix, Pageable pageable);

    List<Memo> findAllByUpdatedAtGreaterThan(LocalDateTime time);

    @Query("SELECT new dev.yunseong.website.blog.domain.SitemapMemoDto(m.id, m.updatedAt) "
            + "FROM Memo m WHERE m.name NOT LIKE '/private%' ORDER BY m.id ASC")
    List<SitemapMemoDto> findPublicSitemapData();
}
