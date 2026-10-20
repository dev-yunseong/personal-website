package dev.yunseong.website.blog.repository;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.domain.SitemapMemoDto;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.show-sql=false"
})
class MemoRepositorySitemapTest {

    @Autowired
    private MemoRepository memoRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findPublicSitemapDataReturnsOrderedMetadataWithoutLoadingEntities() {
        Memo first = memoRepository.saveAndFlush(new Memo("/blog/first", "first memo content"));
        memoRepository.saveAndFlush(new Memo("/private/secret", "private content"));
        Memo second = memoRepository.saveAndFlush(new Memo("/blog/second", "second memo content"));
        entityManager.refresh(first);
        entityManager.refresh(second);
        List<SitemapMemoDto> expected = List.of(
                new SitemapMemoDto(first.getId(), first.getUpdatedAt()),
                new SitemapMemoDto(second.getId(), second.getUpdatedAt()));

        // Discard fixture entities so the first-level cache cannot hide entity loading.
        entityManager.clear();
        Statistics statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        assertEquals(expected, memoRepository.findPublicSitemapData());
        assertEquals(1L, statistics.getPrepareStatementCount());
        assertEquals(0L, statistics.getEntityLoadCount());
    }

    @Test
    void findPublicSitemapDataReturnsEmptyWhenOnlyPrivateMemosExist() {
        memoRepository.saveAndFlush(new Memo("/private/secret", "private content"));
        entityManager.clear();

        assertTrue(memoRepository.findPublicSitemapData().isEmpty());
    }
}
