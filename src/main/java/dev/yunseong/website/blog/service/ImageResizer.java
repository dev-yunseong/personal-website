package dev.yunseong.website.blog.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
import java.util.Iterator;

@Slf4j
@Service
public class ImageResizer {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB in bytes

    /**
     * Checks if the content type represents an image
     */
    public boolean isImage(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * Resizes an image to fit within the maximum file size while maintaining aspect ratio
     *
     * @param originalBytes the original image bytes
     * @param contentType the content type of the image
     * @return the resized image bytes
     * @throws IOException if image processing fails
     */
    public byte[] resizeToFitMaxSize(byte[] originalBytes, String contentType) throws IOException {
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

    /**
     * Checks if the file size exceeds the maximum allowed size
     */
    public boolean exceedsMaxSize(long fileSize) {
        return fileSize > MAX_FILE_SIZE;
    }

    /**
     * Gets the maximum allowed file size
     */
    public long getMaxFileSize() {
        return MAX_FILE_SIZE;
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
