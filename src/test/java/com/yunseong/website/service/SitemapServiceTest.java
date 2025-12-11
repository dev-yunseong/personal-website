package com.yunseong.website.service;

import com.yunseong.website.domain.GameProject;
import com.yunseong.website.domain.Memo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SitemapServiceTest {

    @Mock
    private MemoService memoService;

    @Mock
    private GameProjectService gameProjectService;

    @InjectMocks
    private SitemapService sitemapService;

    private Memo testMemo1;
    private Memo testMemo2;
    private GameProject testGame1;
    private GameProject testGame2;

    @BeforeEach
    void setUp() {
        testMemo1 = new Memo(1L, "/blog/post1", "Content 1", 
            LocalDateTime.of(2024, 1, 1, 0, 0), 
            LocalDateTime.of(2024, 1, 2, 0, 0));
        testMemo2 = new Memo(2L, "/blog/post2", "Content 2", 
            LocalDateTime.of(2024, 2, 1, 0, 0), 
            LocalDateTime.of(2024, 2, 2, 0, 0));
        
        testGame1 = new GameProject(1L, "https://game1.example.com", testMemo1);
        testGame2 = new GameProject(2L, "https://game2.example.com", testMemo2);
    }

    @Test
    void generateSitemap_IncludesRootUrl() {
        // Given
        when(memoService.getMemos(any(Pageable.class)))
            .thenReturn(new PageImpl<>(Arrays.asList()));
        when(gameProjectService.getAllGameProjects())
            .thenReturn(Arrays.asList());

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
        when(memoService.getMemos(any(Pageable.class)))
            .thenReturn(new PageImpl<>(Arrays.asList()));
        when(gameProjectService.getAllGameProjects())
            .thenReturn(Arrays.asList());

        // When
        String sitemap = sitemapService.generateSitemap("https://example.com");

        // Then
        assertTrue(sitemap.contains("<loc>https://example.com/public/memos</loc>"));
    }

    @Test
    void generateSitemap_IncludesPublicGamesListPage() {
        // Given
        when(memoService.getMemos(any(Pageable.class)))
            .thenReturn(new PageImpl<>(Arrays.asList()));
        when(gameProjectService.getAllGameProjects())
            .thenReturn(Arrays.asList());

        // When
        String sitemap = sitemapService.generateSitemap("https://example.com");

        // Then
        assertTrue(sitemap.contains("<loc>https://example.com/public/games</loc>"));
    }

    @Test
    void generateSitemap_IncludesAllMemos() {
        // Given
        Page<Memo> memoPage = new PageImpl<>(Arrays.asList(testMemo1, testMemo2));
        when(memoService.getMemos(any(Pageable.class)))
            .thenReturn(memoPage);
        when(gameProjectService.getAllGameProjects())
            .thenReturn(Arrays.asList());

        // When
        String sitemap = sitemapService.generateSitemap("https://example.com");

        // Then
        assertTrue(sitemap.contains("<loc>https://example.com/public/memos/1</loc>"));
        assertTrue(sitemap.contains("<loc>https://example.com/public/memos/2</loc>"));
        assertTrue(sitemap.contains("<lastmod>2024-01-02</lastmod>"));
        assertTrue(sitemap.contains("<lastmod>2024-02-02</lastmod>"));
    }

    @Test
    void generateSitemap_IncludesAllGames() {
        // Given
        when(memoService.getMemos(any(Pageable.class)))
            .thenReturn(new PageImpl<>(Arrays.asList()));
        when(gameProjectService.getAllGameProjects())
            .thenReturn(Arrays.asList(testGame1, testGame2));

        // When
        String sitemap = sitemapService.generateSitemap("https://example.com");

        // Then
        assertTrue(sitemap.contains("<loc>https://example.com/public/games/1</loc>"));
        assertTrue(sitemap.contains("<loc>https://example.com/public/games/2</loc>"));
    }

    @Test
    void generateSitemap_HandlesMultiplePages() {
        // Given - First page has 1 item and indicates there's another page
        List<Memo> firstPageContent = Arrays.asList(testMemo1);
        Page<Memo> firstPage = mock(Page.class);
        when(firstPage.getContent()).thenReturn(firstPageContent);
        when(firstPage.hasNext()).thenReturn(true);
        
        // Second page has 1 item and indicates no more pages
        List<Memo> secondPageContent = Arrays.asList(testMemo2);
        Page<Memo> secondPage = mock(Page.class);
        when(secondPage.getContent()).thenReturn(secondPageContent);
        when(secondPage.hasNext()).thenReturn(false);
        
        when(memoService.getMemos(any(Pageable.class)))
            .thenReturn(firstPage)
            .thenReturn(secondPage);
        when(gameProjectService.getAllGameProjects())
            .thenReturn(Arrays.asList());

        // When
        String sitemap = sitemapService.generateSitemap("https://example.com");

        // Then
        assertTrue(sitemap.contains("<loc>https://example.com/public/memos/1</loc>"));
        assertTrue(sitemap.contains("<loc>https://example.com/public/memos/2</loc>"));
    }

    @Test
    void generateSitemap_EscapesSpecialCharacters() {
        // Given
        Memo memoWithSpecialChars = new Memo(3L, "/blog/post&test<>", "Content", 
            LocalDateTime.now(), LocalDateTime.now());
        when(memoService.getMemos(any(Pageable.class)))
            .thenReturn(new PageImpl<>(Arrays.asList(memoWithSpecialChars)));
        when(gameProjectService.getAllGameProjects())
            .thenReturn(Arrays.asList());

        // When
        String sitemap = sitemapService.generateSitemap("https://example.com");

        // Then
        // XML special characters should be escaped
        assertFalse(sitemap.contains("&") && !sitemap.contains("&amp;") && !sitemap.contains("&lt;"));
    }
}
