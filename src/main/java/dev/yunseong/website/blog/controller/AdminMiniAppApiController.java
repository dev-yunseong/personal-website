package dev.yunseong.website.blog.controller;

import dev.yunseong.website.blog.service.MiniAppAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/miniapps")
@RequiredArgsConstructor
public class AdminMiniAppApiController {

    private final MiniAppAiService miniAppAiService;

    @PostMapping("/generate")
    public ResponseEntity<?> generateMetadata(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        String content = body.get("content");

        MiniAppAiService.MiniAppMetadata metadata;

        if (url != null && !url.isBlank()) {
            try {
                metadata = miniAppAiService.generateMetadataFromUrl(url.trim());
            } catch (Exception e) {
                log.warn("Failed to fetch URL for metadata generation: {}", url, e);
                return ResponseEntity.badRequest().body(Map.of("error", "URL에서 콘텐츠를 가져오지 못했습니다: " + e.getMessage()));
            }
        } else if (content != null && !content.isBlank()) {
            metadata = miniAppAiService.generateMetadata(content);
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "HTML content or URL is required"));
        }

        return ResponseEntity.ok(Map.of(
                "title", metadata.title(),
                "description", metadata.description()
        ));
    }
}
