package dev.yunseong.website.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;

    @Value("${s3.bucket-name:}")
    private String bucketName;

    @Value("${s3.endpoint:}")
    private String endpoint;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB in bytes

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
        if (fileSize > MAX_FILE_SIZE && isImage(contentType)) {
            log.info("File size {} exceeds maximum {}. Resizing image...", fileSize, MAX_FILE_SIZE);
            fileBytes = resizeImage(fileBytes, contentType);
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
        if (endpoint != null && !endpoint.isEmpty()) {
            return String.format("%s/%s/%s", endpoint, bucketName, key);
        }
        return String.format("https://%s.s3.amazonaws.com/%s", bucketName, key);
    }

    private boolean isImage(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    private byte[] resizeImage(byte[] originalBytes, String contentType) throws IOException {
        // Read the original image
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(originalBytes));
        if (originalImage == null) {
            throw new IOException("Could not read image data");
        }

        // Get format name from content type
        String formatName = getFormatName(contentType);
        
        // Start with a scale factor and iteratively reduce if necessary
        double scaleFactor = Math.sqrt((double) MAX_FILE_SIZE / originalBytes.length);
        byte[] resizedBytes;
        int attempts = 0;
        int maxAttempts = 10;

        do {
            int newWidth = (int) (originalImage.getWidth() * scaleFactor);
            int newHeight = (int) (originalImage.getHeight() * scaleFactor);

            // Ensure minimum dimensions
            if (newWidth < 1) newWidth = 1;
            if (newHeight < 1) newHeight = 1;

            BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, 
                    originalImage.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_RGB : originalImage.getType());
            
            Graphics2D graphics = resizedImage.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
            graphics.dispose();

            // Write to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            
            // Use compression for JPEG images
            if ("jpeg".equalsIgnoreCase(formatName) || "jpg".equalsIgnoreCase(formatName)) {
                resizedBytes = compressJpeg(resizedImage, baos);
            } else {
                ImageIO.write(resizedImage, formatName, baos);
                resizedBytes = baos.toByteArray();
            }

            // Reduce scale factor for next attempt if still too large
            scaleFactor *= 0.9;
            attempts++;
        } while (resizedBytes.length > MAX_FILE_SIZE && attempts < maxAttempts);

        return resizedBytes;
    }

    private byte[] compressJpeg(BufferedImage image, ByteArrayOutputStream baos) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("No writers found for JPEG format");
        }

        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.85f); // 85% quality
        }

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }

        return baos.toByteArray();
    }

    private String getFormatName(String contentType) {
        if (contentType == null || !contentType.contains("/")) {
            return "jpeg";
        }
        
        String format = contentType.substring(contentType.lastIndexOf("/") + 1).toLowerCase();
        // Normalize format names
        if (format.equals("jpg")) {
            format = "jpeg";
        }
        return format;
    }
}
