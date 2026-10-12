package dev.yunseong.website.global.service;

import dev.yunseong.website.blog.domain.GameProject;
import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.domain.SitemapMemoDto;
import dev.yunseong.website.blog.service.GameProjectService;
import dev.yunseong.website.blog.service.MemoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SitemapServiceTest {

    @Mock
    private MemoService memoService;

    @Mock
    private GameProjectService gameProjectService;

    @InjectMocks
    private SitemapService sitemapService;

    private SitemapMemoDto testMemo1;
    private SitemapMemoDto testMemo2;
    private GameProject testGame1;
    private GameProject testGame2;

    @BeforeEach
    void setUp() {
        testMemo1 = new SitemapMemoDto(1L, LocalDateTime.of(2024, 1, 2, 0, 0));
        testMemo2 = new SitemapMemoDto(2L, LocalDateTime.of(2024, 2, 2, 0, 0));

        Memo fullMemo1 = new Memo(1L, "/blog/post1", "Content 1",
                LocalDateTime.of(2024, 1, 1, 0, 0),
                LocalDateTime.of(2024, 1, 2, 0, 0));
        Memo fullMemo2 = new Memo(2L, "/blog/post2", "Content 2",
                LocalDateTime.of(2024, 2, 1, 0, 0),
                LocalDateTime.of(2024, 2, 2, 0, 0));

        testGame1 = new GameProject(1L, "https://game1.example.com", fullMemo1);
        testGame2 = new GameProject(2L, "https://game2.example.com", fullMemo2);
    }

    @Test
    void generateSitemap_IncludesRootUrl() {
        // Given
        when(memoService.getPublicSitemapData()).thenReturn(Collections.emptyList());
        when(gameProjectService.getAllGameProjects()).thenReturn(Collections.emptyList());

        // When
        String sitemap = sitemapService.generateSitemap("https://example.com");

        // Then
        assertTrue(sitemap.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(sitemap.contains("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"));
        assertTrue(sitemap.contains("<loc>https://example.com</loc>"));
        assertTrue(sitemap.contains("</urlset>"));
    }

    @Test
    void generateSitemap_IncludesPublicMemosListPage() {
        // Given
        when(memoService.getPublicSitemapData()).thenReturn(Collections.emptyList());
        when(gameProjectService.getAllGameProjects()).thenReturn(Collections.emptyList());

        // When
        String sitemap = sitemapService.generateSitemap("https://example.com");

        // Then
        assertTrue(sitemap.contains("<loc>https://example.com/public/memos</loc>"));
    }

    @Test
    void generateSitemap_IncludesPublicProjectsListPage() {
        // Given
        when(memoService.getPublicSitemapData()).thenReturn(Collections.emptyList());
        when(gameProjectService.getAllGameProjects()).thenReturn(Collections.emptyList());

        // When
        String sitemap = sitemapService.generateSitemap("https://example.com");

        // Then
        assertTrue(sitemap.contains("<loc>https://example.com/public/projects</loc>"));
    }

    @Test
    void generateSitemap_IncludesAllMemos() {
        // Given
        when(memoService.getPublicSitemapData()).thenReturn(Arrays.asList(testMemo1, testMemo2));
        when(gameProjectService.getAllGameProjects()).thenReturn(Collections.emptyList());

        // When
        String sitemap = sitemapService.generateSitemap("https://example.com");

        // Then
        assertTrue(sitemap.contains("<loc>https://example.com/public/memos/1</loc>"));
        assertTrue(sitemap.contains("<loc>https://example.com/public/memos/2</loc>"));
        assertTrue(sitemap.contains("<lastmod>2024-01-02</lastmod>"));
        assertTrue(sitemap.contains("<lastmod>2024-02-02</lastmod>"));
    }

    @Test
    void generateSitemap_IncludesAllProjects() {
        // Given
        when(memoService.getPublicSitemapData()).thenReturn(Collections.emptyList());
        when(gameProjectService.getAllGameProjects()).thenReturn(Arrays.asList(testGame1, testGame2));

        // When
        String sitemap = sitemapService.generateSitemap("https://example.com");

        // Then
        assertTrue(sitemap.contains("<loc>https://example.com/public/projects/1</loc>"));
        assertTrue(sitemap.contains("<loc>https://example.com/public/projects/2</loc>"));
    }

    @Test
    void generateSitemap_HandlesMultipleMemos() {
        // Given
        when(memoService.getPublicSitemapData()).thenReturn(Arrays.asList(testMemo1, testMemo2));
        when(gameProjectService.getAllGameProjects()).thenReturn(Collections.emptyList());

        // When
        String sitemap = sitemapService.generateSitemap("https://example.com");

        // Then
        assertTrue(sitemap.contains("<loc>https://example.com/public/memos/1</loc>"));
        assertTrue(sitemap.contains("<loc>https://example.com/public/memos/2</loc>"));
    }

    @Test
    void generateSitemap_EscapesSpecialCharacters() {
        // Given
        when(memoService.getPublicSitemapData()).thenReturn(Collections.emptyList());
        when(gameProjectService.getAllGameProjects()).thenReturn(Collections.emptyList());

        // When
        String sitemap = sitemapService.generateSitemap("https://example.com?param=1&other=2");

        // Then
        assertTrue(sitemap.contains("https://example.com?param=1&amp;other=2"),
                "Ampersands in URLs should be escaped to &amp;");
        assertTrue(sitemap.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(sitemap.contains("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"));
    }
}
