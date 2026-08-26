package dev.yunseong.website.blog.service;

import dev.yunseong.website.blog.domain.Memo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the blog listing order against the database. The order rule lives in the queries
 * ({@code ORDER BY updated_at DESC}); the service deliberately drops any client sort, so these
 * listings must come back newest-updated-first whatever {@code ?sort=} the caller supplies.
 */
@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        // PostgreSQL mode: the category queries use the Postgres regex operator (~)
        "DATABASE_URL=jdbc:h2:mem:memoorder;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "DATABASE_USER=sa",
        "DATABASE_PW=",
        "APPLICATION_USER=admin",
        "APPLICATION_PW=admin"
})
@Import(MemoService.class)
class MemoListingOrderTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 1, 1, 0, 0);

    /** Sorts a caller might send; none of them may change the result. */
    private static final List<Pageable> PAGE_REQUESTS = List.of(
            PageRequest.of(0, 10),
            PageRequest.of(0, 10, Sort.by("updatedAt").ascending()),
            PageRequest.of(0, 10, Sort.by("created_at").descending()),
            PageRequest.of(0, 10, Sort.by("name").ascending())
    );

    @Autowired
    private MemoService memoService;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void seed() {
        // created_at is deliberately the reverse of updated_at, so a created_at sort is visible.
        insert(1L, "/java", BASE.plusDays(5), BASE.plusHours(1));
        insert(2L, "/java/alpha", BASE.plusDays(4), BASE.plusHours(2));
        insert(3L, "/java/beta", BASE.plusDays(3), BASE.plusHours(3));
        insert(4L, "/java/gamma", BASE.plusDays(2), BASE.plusHours(4));
        insert(5L, "/private/secret", BASE.plusDays(1), BASE.plusHours(5));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void publicCategoryListing_isUpdatedAtDesc_forEverySortParameter() {
        for (Pageable pageable : PAGE_REQUESTS) {
            assertThat(names(memoService.getPublicMemos("/java", pageable)))
                    .as("sort=%s", pageable.getSort())
                    .containsExactly("/java/gamma", "/java/beta", "/java/alpha", "/java");
        }
    }

    @Test
    void categoryListing_isUpdatedAtDesc_forEverySortParameter() {
        for (Pageable pageable : PAGE_REQUESTS) {
            assertThat(names(memoService.getMemos("/java", pageable)))
                    .as("sort=%s", pageable.getSort())
                    .containsExactly("/java/gamma", "/java/beta", "/java/alpha", "/java");
        }
    }

    @Test
    void publicListing_isUpdatedAtDesc_forEverySortParameter() {
        for (Pageable pageable : PAGE_REQUESTS) {
            assertThat(names(memoService.getPublicMemos(pageable)))
                    .as("sort=%s", pageable.getSort())
                    .containsExactly("/java/gamma", "/java/beta", "/java/alpha", "/java");
        }
    }

    @Test
    void listing_isUpdatedAtDesc_forEverySortParameter() {
        for (Pageable pageable : PAGE_REQUESTS) {
            assertThat(names(memoService.getMemos(pageable)))
                    .as("sort=%s", pageable.getSort())
                    .containsExactly("/private/secret", "/java/gamma", "/java/beta", "/java/alpha", "/java");
        }
    }

    @Test
    void search_isUpdatedAtDesc_forEverySortParameter() {
        for (Pageable pageable : PAGE_REQUESTS) {
            assertThat(names(memoService.searchPublicMemo("java", pageable)))
                    .as("sort=%s", pageable.getSort())
                    .containsExactly("/java/gamma", "/java/beta", "/java/alpha", "/java");
        }
    }

    private List<String> names(org.springframework.data.domain.Page<Memo> page) {
        return page.getContent().stream().map(Memo::getName).toList();
    }

    private void insert(long id, String name, LocalDateTime createdAt, LocalDateTime updatedAt) {
        entityManager.getEntityManager()
                .createNativeQuery("INSERT INTO memos (id, name, content, created_at, updated_at) VALUES (?,?,?,?,?)")
                .setParameter(1, id)
                .setParameter(2, name)
                .setParameter(3, "content")
                .setParameter(4, createdAt)
                .setParameter(5, updatedAt)
                .executeUpdate();
    }
}
