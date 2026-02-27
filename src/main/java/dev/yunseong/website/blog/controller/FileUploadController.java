package dev.yunseong.website.blog.controller;

import dev.yunseong.website.blog.service.S3StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final S3StorageService s3StorageService;

    @PostMapping("/file")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            String contentType = file.getContentType();
            String fileUrl = s3StorageService.uploadFile(file);

            String markdown;
            if (contentType != null && contentType.startsWith("image/")) {
                markdown = String.format("![%s](%s)", file.getOriginalFilename(), fileUrl);
            } else if (contentType != null && contentType.startsWith("video/")) {
                markdown = String.format("<video controls>\n  <source src=\"%s\" type=\"%s\">\n  Your browser does not support the video tag.\n</video>", fileUrl, contentType);
            } else {
                markdown = String.format("[%s](%s)", file.getOriginalFilename(), fileUrl);
            }

            Map<String, String> response = new HashMap<>();
            response.put("url", fileUrl);
            response.put("markdown", markdown);

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload file: " + e.getMessage()));
        }
    }
}
