package dev.yunseong.website.manage.controller;

import dev.yunseong.website.manage.domain.MemoTrafficStat;
import dev.yunseong.website.manage.service.ContentPopularityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Content popularity for the admin console. Secured by the {@code /api/admin/**}
 * rule in {@code WebSecurityConfig}.
 */
@RestController
@RequestMapping("/api/admin/console/content")
@RequiredArgsConstructor
public class ContentPopularityApiController {

    private static final int TOP_CONTENT_LIMIT = 20;

    private final ContentPopularityService contentPopularityService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> content(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "false") boolean includeBots) {
        List<MemoTrafficStat> popularContent =
                contentPopularityService.getPopularContentForLastDays(days, includeBots);
        return ResponseEntity.ok(Map.of(
                "content", popularContent.stream().limit(TOP_CONTENT_LIMIT).toList(),
                "categories", contentPopularityService.summarizeCategories(popularContent)
        ));
    }
}
