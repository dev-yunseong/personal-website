package dev.yunseong.website.global.service;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.domain.SitemapMemoDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SitemapBenchmarkTest {

    @Test
    void benchmarkProjectionVsEntityAllocation() {
        int recordCount = 10_000;
        String largeContent = "a".repeat(100_000); // 100KB body text per memo
        LocalDateTime now = LocalDateTime.now();

        // Measure memory and execution for Full Entity simulation
        System.gc();
        long memBeforeEntity = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
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
        long memAfterEntity = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long entityAllocatedBytes = Math.max(0, memAfterEntity - memBeforeEntity);

        // Clear entity references and run GC
        entities.clear();
        entitySitemap.setLength(0);
        entities = null;
        entitySitemap = null;
        System.gc();

        // Measure memory and execution for DTO Projection simulation
        long memBeforeDto = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
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
        long memAfterDto = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long dtoAllocatedBytes = Math.max(0, memAfterDto - memBeforeDto);

        System.out.println("=== Benchmark Results (" + recordCount + " Memos) ===");
        System.out.println("Entity approach duration: " + entityDurationMs + " ms, Allocated memory: " + (entityAllocatedBytes / (1024 * 1024)) + " MB");
        System.out.println("Projection DTO duration: " + dtoDurationMs + " ms, Allocated memory: " + (dtoAllocatedBytes / (1024 * 1024)) + " MB");

        // The DTO projection memory consumption should be substantially smaller
        assertTrue(dtoAllocatedBytes <= entityAllocatedBytes,
                "DTO projection memory allocation (" + dtoAllocatedBytes + ") should be significantly less than or equal to full entity allocation (" + entityAllocatedBytes + ")");
    }
}
