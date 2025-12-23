package dev.yunseong.website.controller;

import dev.yunseong.website.service.S3StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileUploadController.class)
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
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
}
