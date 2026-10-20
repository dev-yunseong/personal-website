package dev.yunseong.website.global.service;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.domain.SitemapMemoDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SitemapBenchmarkTest {

    @Test
    void benchmarkProjectionVsEntityAllocation() {
        int recordCount = 1_000;
        String largeContent = "a".repeat(10_000);
        LocalDateTime now = LocalDateTime.now();

        long startEntityTime = System.nanoTime();
        List<Memo> entities = new ArrayList<>(recordCount);
        for (long i = 1; i <= recordCount; i++) {
            entities.add(new Memo(i, "/blog/memo-" + i, largeContent, now, now));
        }
        StringBuilder entitySitemap = new StringBuilder();
        for (Memo memo : entities) {
            entitySitemap.append(memo.getId()).append(memo.getUpdatedAt());
        }
        long entityDurationMs = (System.nanoTime() - startEntityTime) / 1_000_000;

        long startDtoTime = System.nanoTime();
        List<SitemapMemoDto> dtos = new ArrayList<>(recordCount);
        for (long i = 1; i <= recordCount; i++) {
            dtos.add(new SitemapMemoDto(i, now));
        }
        StringBuilder dtoSitemap = new StringBuilder();
        for (SitemapMemoDto memo : dtos) {
            dtoSitemap.append(memo.id()).append(memo.updatedAt());
        }
        long dtoDurationMs = (System.nanoTime() - startDtoTime) / 1_000_000;

        System.out.println("=== Benchmark Summary (" + recordCount + " Memos) ===");
        System.out.println("Full Entity construct & format time: " + entityDurationMs + " ms");
        System.out.println("Projection DTO construct & format time: " + dtoDurationMs + " ms");

        // Verify output equivalence
        assertEquals(entitySitemap.toString(), dtoSitemap.toString());
        assertFalse(dtos.isEmpty());
    }
}
