package dev.yunseong.website.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService {

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
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String key = UUID.randomUUID().toString() + extension;

        byte[] fileBytes = file.getBytes();
        long fileSize = fileBytes.length;
        String contentType = file.getContentType();

        // Check if file needs resizing
        if (imageResizer.exceedsMaxSize(fileSize) && imageResizer.isImage(contentType)) {
            log.info("File size {} exceeds maximum {}. Resizing image...", fileSize, imageResizer.getMaxFileSize());
            fileBytes = imageResizer.resizeToFitMaxSize(fileBytes, contentType);
            log.info("Image resized. New size: {}", fileBytes.length);
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
