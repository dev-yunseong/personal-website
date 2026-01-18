package dev.yunseong.website.blog.controller;

import dev.yunseong.website.ai.tool.BlogTools;
import dev.yunseong.website.blog.service.S3StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "s3.endpoint=http://localhost:9000",
    "s3.region=us-east-1",
    "s3.access-key=test",
    "s3.secret-key=test",
    "s3.bucket-name=test-bucket",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.security.user.name=admin",
    "spring.security.user.password=admin"
})
class FileUploadControllerTest {

    @MockitoBean
    private BlogTools blogTools;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private S3Client s3Client;

    @MockitoBean
    private S3StorageService s3StorageService;

    @Test
    @WithMockUser
    void uploadImage_Success() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "test content".getBytes()
        );
        String expectedUrl = "http://localhost:9000/test-bucket/test-image.jpg";
        when(s3StorageService.uploadFile(any())).thenReturn(expectedUrl);

        // When & Then
        mockMvc.perform(multipart("/admin/upload/image")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.url").value(expectedUrl))
                .andExpect(jsonPath("$.markdown").value("![test-image.jpg](" + expectedUrl + ")"));
    }

    @Test
    @WithMockUser
    void uploadImage_EmptyFile() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                new byte[0]
        );

        // When & Then
        mockMvc.perform(multipart("/admin/upload/image")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("File is empty"));
    }

    @Test
    @WithMockUser
    void uploadImage_NotAnImage() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-file.txt",
                "text/plain",
                "test content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/admin/upload/image")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Only image files are allowed"));
    }

    @Test
    @WithMockUser
    void uploadImage_ServiceException() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "test content".getBytes()
        );
        when(s3StorageService.uploadFile(any())).thenThrow(new IOException("Storage error"));

        // When & Then
        mockMvc.perform(multipart("/admin/upload/image")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to upload file: Storage error"));
    }

    @Test
    @WithMockUser
    void uploadPdf_Success() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                "test pdf content".getBytes()
        );
        String expectedUrl = "http://localhost:9000/test-bucket/test-document.pdf";
        when(s3StorageService.uploadFile(any())).thenReturn(expectedUrl);

        // When & Then
        mockMvc.perform(multipart("/admin/upload/pdf")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.url").value(expectedUrl))
                .andExpect(jsonPath("$.markdown").value("[test-document.pdf](" + expectedUrl + ")"));
    }

    @Test
    @WithMockUser
    void uploadPdf_EmptyFile() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-document.pdf",
                "application/pdf",
                new byte[0]
        );

        // When & Then
        mockMvc.perform(multipart("/admin/upload/pdf")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("File is empty"));
    }

    @Test
    @WithMockUser
    void uploadPdf_NotAPdf() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-file.txt",
                "text/plain",
                "test content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/admin/upload/pdf")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Only PDF files are allowed"));
    }

    @Test
    @WithMockUser
    void uploadVideo_Success_Mp4() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-video.mp4",
                "video/mp4",
                "test video content".getBytes()
        );
        String expectedUrl = "http://localhost:9000/test-bucket/test-video.mp4";
        when(s3StorageService.uploadFile(any())).thenReturn(expectedUrl);

        // When & Then
        mockMvc.perform(multipart("/admin/upload/video")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.url").value(expectedUrl))
                .andExpect(jsonPath("$.markdown").value("<video controls>\n  <source src=\"" + expectedUrl + "\" type=\"video/mp4\">\n  Your browser does not support the video tag.\n</video>"));
    }

    @Test
    @WithMockUser
    void uploadVideo_Success_Mov() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-video.mov",
                "video/quicktime",
                "test video content".getBytes()
        );
        String expectedUrl = "http://localhost:9000/test-bucket/test-video.mov";
        when(s3StorageService.uploadFile(any())).thenReturn(expectedUrl);

        // When & Then
        mockMvc.perform(multipart("/admin/upload/video")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.url").value(expectedUrl))
                .andExpect(jsonPath("$.markdown").value("<video controls>\n  <source src=\"" + expectedUrl + "\" type=\"video/quicktime\">\n  Your browser does not support the video tag.\n</video>"));
    }

    @Test
    @WithMockUser
    void uploadVideo_EmptyFile() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-video.mp4",
                "video/mp4",
                new byte[0]
        );

        // When & Then
        mockMvc.perform(multipart("/admin/upload/video")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("File is empty"));
    }

    @Test
    @WithMockUser
    void uploadVideo_NotAVideo() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-file.txt",
                "text/plain",
                "test content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/admin/upload/video")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Only MP4 and MOV video files are allowed"));
    }
}
