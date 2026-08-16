package dev.yunseong.website.blog.controller;

import dev.yunseong.website.ai.tool.BlogTools;
import dev.yunseong.website.blog.service.MiniAppAiService;
import dev.yunseong.website.blog.service.S3StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    "spring.security.user.password=admin",
    "spring.ai.openai.api-key=test"
})
class AdminMiniAppApiControllerTest {

    @MockitoBean
    private BlogTools blogTools;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private S3Client s3Client;

    @MockitoBean
    private S3StorageService s3StorageService;

    @MockitoBean
    private MiniAppAiService miniAppAiService;

    @Test
    @WithMockUser
    void generateMetadata_WithContent_ReturnsMetadata() throws Exception {
        when(miniAppAiService.generateMetadata(anyString()))
                .thenReturn(new MiniAppAiService.MiniAppMetadata("Test App", "테스트 앱 설명입니다."));

        mockMvc.perform(post("/api/admin/miniapps/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\": \"<html><body>Hello</body></html>\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test App"))
                .andExpect(jsonPath("$.description").value("테스트 앱 설명입니다."));
    }

    @Test
    @WithMockUser
    void generateMetadata_WithUrl_ReturnsMetadata() throws Exception {
        when(miniAppAiService.generateMetadataFromUrl(anyString()))
                .thenReturn(new MiniAppAiService.MiniAppMetadata("URL App", "URL 앱 설명입니다."));

        mockMvc.perform(post("/api/admin/miniapps/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://example.com/app/index.html\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("URL App"))
                .andExpect(jsonPath("$.description").value("URL 앱 설명입니다."));
    }

    @Test
    @WithMockUser
    void generateMetadata_WithUrlFetchFailure_ReturnsBadRequest() throws Exception {
        when(miniAppAiService.generateMetadataFromUrl(anyString()))
                .thenThrow(new RuntimeException("Failed to fetch URL: HTTP 404"));

        mockMvc.perform(post("/api/admin/miniapps/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://example.com/notfound\"}")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser
    void generateMetadata_WithNeitherContentNorUrl_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/admin/miniapps/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @WithMockUser
    void generateMetadata_UrlTakesPrecedenceOverContent() throws Exception {
        when(miniAppAiService.generateMetadataFromUrl(anyString()))
                .thenReturn(new MiniAppAiService.MiniAppMetadata("URL App", "URL 앱 설명입니다."));

        mockMvc.perform(post("/api/admin/miniapps/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://example.com/app\", \"content\": \"<html>ignored</html>\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("URL App"));
    }
}
