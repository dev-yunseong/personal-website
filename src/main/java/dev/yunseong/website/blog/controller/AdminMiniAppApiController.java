package dev.yunseong.website.blog.controller;

import dev.yunseong.website.blog.service.MiniAppAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/miniapps")
@RequiredArgsConstructor
public class AdminMiniAppApiController {

    private final MiniAppAiService miniAppAiService;

    @PostMapping("/generate")
    public ResponseEntity<?> generateMetadata(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "HTML content is required"));
        }
        MiniAppAiService.MiniAppMetadata metadata = miniAppAiService.generateMetadata(content);
        return ResponseEntity.ok(Map.of(
                "title", metadata.title(),
                "description", metadata.description()
        ));
    }
}
