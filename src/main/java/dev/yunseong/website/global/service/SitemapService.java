package dev.yunseong.website.global.service;

import dev.yunseong.website.blog.service.GameProjectService;
import dev.yunseong.website.blog.service.MemoService;
import dev.yunseong.website.blog.domain.GameProject;
import dev.yunseong.website.blog.domain.Memo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
        
        // Add all memo URLs
        List<Memo> allMemos = getAllMemos();
        for (Memo memo : allMemos) {
            String url = baseUrl + "/public/memos/" + memo.getId();
            addUrl(sitemap, url, memo.getUpdatedAt() != null ? 
                memo.getUpdatedAt().format(DATE_FORMATTER) : null);
        }
        
        // Add public games list page
        addUrl(sitemap, baseUrl + "/public/games", null);
        
        // Add all game URLs
        List<GameProject> allGames = gameProjectService.getAllGameProjects();
        for (GameProject game : allGames) {
            String url = baseUrl + "/public/games/" + game.getId();
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
    
    private List<Memo> getAllMemos() {
        List<Memo> allMemos = new ArrayList<>();
        int page = 0;
        int pageSize = 100;
        Page<Memo> memoPage;
        
        do {
            Pageable pageable = PageRequest.of(page, pageSize);
            memoPage = memoService.getMemos(pageable);
            allMemos.addAll(memoPage.getContent());
            page++;
        } while (memoPage.hasNext());
        
        return allMemos;
    }
}
