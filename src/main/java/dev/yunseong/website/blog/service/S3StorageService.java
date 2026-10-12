package dev.yunseong.website.blog.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService {

    private static final Map<String, Set<String>> ALLOWED_EXTENSIONS_MIME = Map.of(
            ".jpg", Set.of("image/jpeg"),
            ".jpeg", Set.of("image/jpeg"),
            ".png", Set.of("image/png"),
            ".gif", Set.of("image/gif"),
            ".webp", Set.of("image/webp"),
            ".mp4", Set.of("video/mp4"),
            ".webm", Set.of("video/webm"),
            ".mov", Set.of("video/quicktime"),
            ".pdf", Set.of("application/pdf"),
            ".txt", Set.of("text/plain")
    );

    private final S3Client s3Client;
    private final ImageResizer imageResizer;

    @Value("${s3.bucket-name:}")
    private String bucketName;

    @Value("${s3.endpoint:}")
    private String endpoint;

    @Value("${s3.public-url:}")
    private String publicUrl;

    public String uploadFile(MultipartFile file) throws IOException {
        if (bucketName == null || bucketName.isEmpty()) {
            throw new IllegalStateException("S3 bucket name is not configured");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }

        String contentType = file.getContentType();
        validateFile(extension, contentType);

        String key = UUID.randomUUID().toString() + extension;

        byte[] fileBytes = file.getBytes();
        long fileSize = fileBytes.length;

        // Resize images to appropriate size (dimension and file size limits)
        if (imageResizer.isImage(contentType)) {
            log.info("Resizing image if needed (size={}, contentType={})...", fileSize, contentType);
            fileBytes = imageResizer.resize(fileBytes, contentType);
            log.info("Image processed. Final size: {}", fileBytes.length);
        }

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));

        log.info("File uploaded successfully: {}", key);

        return generateFileUrl(key);
    }

    private void validateFile(String extension, String contentType) {
        if (extension.isEmpty() || contentType == null) {
            throw new IllegalArgumentException("File extension and content type must not be null or empty");
        }

        Set<String> allowedMimes = ALLOWED_EXTENSIONS_MIME.get(extension);
        if (allowedMimes == null || !allowedMimes.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("File extension or content type is not allowed");
        }
    }

    private String generateFileUrl(String key) {
        // Use public URL if configured, otherwise fall back to endpoint or AWS S3 URL
        if (publicUrl != null && !publicUrl.isEmpty()) {
            return String.format("%s/%s/%s", publicUrl, bucketName, key);
        }
        if (endpoint != null && !endpoint.isEmpty()) {
            return String.format("%s/%s/%s", endpoint, bucketName, key);
        }
        return String.format("https://%s.s3.amazonaws.com/%s", bucketName, key);
    }
}
