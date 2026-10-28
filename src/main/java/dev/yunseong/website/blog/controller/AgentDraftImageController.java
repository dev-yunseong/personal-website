package dev.yunseong.website.blog.controller;

import dev.yunseong.website.blog.service.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class AgentDraftImageController {
    private static final Set<String> IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/gif", "image/webp");
    private final S3StorageService storage;

    @PostMapping(value = "/api/agent/drafts/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty() || file.getContentType() == null || !IMAGE_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PNG, JPEG, GIF, WebP 이미지를 선택하세요.");
        }
        try {
            String url = storage.uploadFile(file);
            return Map.of("url", url, "markdown", "![image](" + url + ")");
        } catch (IllegalArgumentException | IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 형식 또는 파일 내용을 확인하세요.", e);
        }
    }
}
