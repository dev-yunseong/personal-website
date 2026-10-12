package dev.yunseong.website.global.service;

import dev.yunseong.website.blog.service.GameProjectService;
import dev.yunseong.website.blog.service.MemoService;
import dev.yunseong.website.blog.domain.GameProject;
import dev.yunseong.website.blog.domain.SitemapMemoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SitemapService {
    
    private final MemoService memoService;
    private final GameProjectService gameProjectService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    
    public String generateSitemap(String baseUrl) {
        StringBuilder sitemap = new StringBuilder();
        sitemap.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sitemap.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        
        // Add root URL
        addUrl(sitemap, baseUrl, null);
        
        // Add public memos list page
        addUrl(sitemap, baseUrl + "/public/memos", null);

        // Add the briefing digest page. Only the page itself: the briefings behind it
        // are private memos, so they never reach the memo loop below, and dated URLs
        // would be a crawl of one page per day with no separate content.
        addUrl(sitemap, baseUrl + "/briefing", null);
        
        // Add all memo URLs
        List<SitemapMemoDto> memos = memoService.getPublicSitemapData();
        for (SitemapMemoDto memo : memos) {
            String url = baseUrl + "/public/memos/" + memo.id();
            addUrl(sitemap, url, memo.updatedAt() != null ?
                memo.updatedAt().format(DATE_FORMATTER) : null);
        }
        
        // Add public projects list page
        addUrl(sitemap, baseUrl + "/public/projects", null);
        
        // Add all project URLs
        List<GameProject> allGames = gameProjectService.getAllGameProjects();
        for (GameProject game : allGames) {
            String url = baseUrl + "/public/projects/" + game.getId();
            addUrl(sitemap, url, null);
        }
        
        sitemap.append("</urlset>");
        return sitemap.toString();
    }
    
    private void addUrl(StringBuilder sitemap, String loc, String lastmod) {
        sitemap.append("  <url>\n");
        sitemap.append("    <loc>").append(escapeXml(loc)).append("</loc>\n");
        if (lastmod != null) {
            sitemap.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        }
        sitemap.append("  </url>\n");
    }
    
    private String escapeXml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
    
}
