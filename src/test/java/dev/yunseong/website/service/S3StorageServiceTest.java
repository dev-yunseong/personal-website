package dev.yunseong.website.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock
    private S3Client s3Client;

    private S3StorageService s3StorageService;

    @BeforeEach
    void setUp() {
        s3StorageService = new S3StorageService(s3Client);
        ReflectionTestUtils.setField(s3StorageService, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(s3StorageService, "endpoint", "http://localhost:9000");
    }

    @Test
    void uploadFile_Success() throws IOException {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "test content".getBytes()
        );

        // When
        String result = s3StorageService.uploadFile(file);

        // Then
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        assertTrue(result.startsWith("http://localhost:9000/test-bucket/"));
        assertTrue(result.endsWith(".jpg"));
    }

    @Test
    void uploadFile_WithoutExtension() throws IOException {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image",
                "image/jpeg",
                "test content".getBytes()
        );

        // When
        String result = s3StorageService.uploadFile(file);

        // Then
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        assertNotNull(result);
        assertTrue(result.startsWith("http://localhost:9000/test-bucket/"));
    }

    @Test
    void uploadFile_NoBucketName() {
        // Given
        ReflectionTestUtils.setField(s3StorageService, "bucketName", "");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "test content".getBytes()
        );

        // When & Then
        assertThrows(IllegalStateException.class, () -> s3StorageService.uploadFile(file));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadFile_VerifyPutObjectRequest() throws IOException {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.png",
                "image/png",
                "test content".getBytes()
        );

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

        // When
        s3StorageService.uploadFile(file);

        // Then
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        PutObjectRequest capturedRequest = requestCaptor.getValue();
        assertEquals("test-bucket", capturedRequest.bucket());
        assertEquals("image/png", capturedRequest.contentType());
        assertTrue(capturedRequest.key().endsWith(".png"));
    }
}
