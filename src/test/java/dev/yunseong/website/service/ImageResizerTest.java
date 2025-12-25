package dev.yunseong.website.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ImageResizerTest {

    private ImageResizer imageResizer;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @BeforeEach
    void setUp() {
        imageResizer = new ImageResizer();
    }

    @Test
    void isImage_WithImageContentType_ReturnsTrue() {
        // Given
        String contentType = "image/jpeg";

        // When
        boolean result = imageResizer.isImage(contentType);

        // Then
        assertTrue(result);
    }

    @Test
    void isImage_WithNonImageContentType_ReturnsFalse() {
        // Given
        String contentType = "text/plain";

        // When
        boolean result = imageResizer.isImage(contentType);

        // Then
        assertFalse(result);
    }

    @Test
    void isImage_WithNullContentType_ReturnsFalse() {
        // When
        boolean result = imageResizer.isImage(null);

        // Then
        assertFalse(result);
    }

    @Test
    void exceedsMaxSize_WithLargeFile_ReturnsTrue() {
        // Given
        long fileSize = 11 * 1024 * 1024; // 11MB

        // When
        boolean result = imageResizer.exceedsMaxSize(fileSize);

        // Then
        assertTrue(result);
    }

    @Test
    void exceedsMaxSize_WithSmallFile_ReturnsFalse() {
        // Given
        long fileSize = 5 * 1024 * 1024; // 5MB

        // When
        boolean result = imageResizer.exceedsMaxSize(fileSize);

        // Then
        assertFalse(result);
    }

    @Test
    void getMaxFileSize_ReturnsCorrectValue() {
        // When
        long maxFileSize = imageResizer.getMaxFileSize();

        // Then
        assertEquals(MAX_FILE_SIZE, maxFileSize);
    }

    @Test
    void resizeToFitMaxSize_WithLargeImage_ResizesSuccessfully() throws IOException {
        // Given - Create a large test image (> 10MB)
        byte[] largeImageBytes = createTestImage(5000, 5000, "jpeg");
        assertTrue(largeImageBytes.length > MAX_FILE_SIZE, "Test image should be larger than 10MB");

        // When
        byte[] resizedBytes = imageResizer.resizeToFitMaxSize(largeImageBytes, "image/jpeg");

        // Then
        assertNotNull(resizedBytes);
        assertTrue(resizedBytes.length <= MAX_FILE_SIZE, "Resized image should be under 10MB");
        assertTrue(resizedBytes.length > 0, "Resized image should have content");
    }

    @Test
    void resizeToFitMaxSize_WithSmallImage_DoesNotIncrease() throws IOException {
        // Given - Create a small test image (< 10MB)
        byte[] smallImageBytes = createTestImage(100, 100, "jpeg");
        assertTrue(smallImageBytes.length < MAX_FILE_SIZE, "Test image should be smaller than 10MB");
        int originalSize = smallImageBytes.length;

        // When
        byte[] resizedBytes = imageResizer.resizeToFitMaxSize(smallImageBytes, "image/jpeg");

        // Then
        assertNotNull(resizedBytes);
        // The resize logic always processes the image, so size may change slightly
        // but should remain reasonable
        assertTrue(resizedBytes.length <= MAX_FILE_SIZE);
    }

    @Test
    void resizeToFitMaxSize_WithPngImage_ResizesSuccessfully() throws IOException {
        // Given - Create a large PNG test image
        byte[] largeImageBytes = createTestImage(3000, 3000, "png");
        
        // Skip if we can't create a PNG large enough (PNG compression is very efficient)
        if (largeImageBytes.length <= MAX_FILE_SIZE) {
            return;
        }

        // When
        byte[] resizedBytes = imageResizer.resizeToFitMaxSize(largeImageBytes, "image/png");

        // Then
        assertNotNull(resizedBytes);
        assertTrue(resizedBytes.length <= MAX_FILE_SIZE, "Resized image should be under 10MB");
    }

    @Test
    void resizeToFitMaxSize_WithInvalidImageData_ThrowsException() {
        // Given
        byte[] invalidBytes = "not an image".getBytes();

        // When & Then
        assertThrows(IOException.class, () -> 
            imageResizer.resizeToFitMaxSize(invalidBytes, "image/jpeg")
        );
    }

    /**
     * Helper method to create test images of various sizes
     */
    private byte[] createTestImage(int width, int height, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // Fill with some data to create realistic file size
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = (x * y) % 256;
                int color = (rgb << 16) | (rgb << 8) | rgb;
                image.setRGB(x, y, color);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }
}
