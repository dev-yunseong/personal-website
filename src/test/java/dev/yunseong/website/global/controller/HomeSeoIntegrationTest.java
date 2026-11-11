package dev.yunseong.website.global.controller;

import dev.yunseong.website.ai.tool.BlogTools;
import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.repository.MemoRepository;
import dev.yunseong.website.blog.service.S3StorageService;
import dev.yunseong.website.global.domain.Profile;
import dev.yunseong.website.global.domain.ProfileLoadResult;
import dev.yunseong.website.global.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "s3.endpoint=http://localhost:9000",
        "s3.region=us-east-1",
        "s3.access-key=test",
        "s3.secret-key=test",
        "s3.bucket-name=test-bucket",
        "app.base-url=https://yunseong.dev",
        "spring.datasource.url=jdbc:h2:mem:homeseodb;MODE=PostgreSQL"
})
class HomeSeoIntegrationTest {

    @MockitoBean
    private BlogTools blogTools;

    @MockitoBean
    private S3Client s3Client;

    @MockitoBean
    private S3StorageService s3StorageService;

    @MockitoBean
    private ProfileService profileService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemoRepository memoRepository;

    @BeforeEach
    void clearMemos() {
        memoRepository.deleteAll();
    }

    @Test
    void home_RendersTitleCanonicalShareImageAndJsonLd() throws Exception {
        when(profileService.getProfile()).thenReturn(ProfileLoadResult.available(profile()));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("(Yunseong Jeong) | Backend Developer</title>")))
                .andExpect(content().string(containsString("rel=\"canonical\"")))
                .andExpect(content().string(containsString("href=\"https://yunseong.dev\"")))
                .andExpect(content().string(containsString("content=\"https://yunseong.dev/images/og-default.png\"")))
                .andExpect(content().string(containsString("content=\"summary_large_image\"")))
                .andExpect(content().string(containsString("<script type=\"application/ld+json\">")))
                .andExpect(content().string(containsString("\"@type\":\"Person\"")))
                .andExpect(content().string(containsString("\"sameAs\":[\"https://github.com/dev-yunseong\"]")));
    }

    @Test
    void home_OmitsJsonLdWhenProfileIsMissing() throws Exception {
        when(profileService.getProfile()).thenReturn(ProfileLoadResult.missing());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("application/ld+json"))));
    }

    @Test
    void memoList_RendersItsOwnCanonical() throws Exception {
        mockMvc.perform(get("/public/memos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("rel=\"canonical\"")))
                .andExpect(content().string(containsString("href=\"https://yunseong.dev/public/memos\"")));
    }

    @Test
    void memoList_KeepsPageAndSizeInCanonicalAfterFirstPage() throws Exception {
        memoRepository.saveAndFlush(new Memo("/notes/seo-first", "first body"));
        memoRepository.saveAndFlush(new Memo("/notes/seo-second", "second body"));

        mockMvc.perform(get("/public/memos").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "href=\"https://yunseong.dev/public/memos?page=1&amp;size=1\"")));
    }

    @Test
    void memoList_DropsPageFromCanonicalOnFirstPage() throws Exception {
        memoRepository.saveAndFlush(new Memo("/notes/seo-first", "first body"));
        memoRepository.saveAndFlush(new Memo("/notes/seo-second", "second body"));

        mockMvc.perform(get("/public/memos").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"https://yunseong.dev/public/memos\"")));
    }

    @Test
    void pageWithoutPageUrl_OmitsCanonical() throws Exception {
        mockMvc.perform(get("/public/chat"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("rel=\"canonical\""))));
    }

    @Test
    void defaultShareImage_IsPublic() throws Exception {
        mockMvc.perform(get("/images/og-default.png"))
                .andExpect(status().isOk());
    }

    private Profile profile() {
        return new Profile(
                "Yunseong Jeong",
                "Software Engineer",
                "Busan, KR",
                "Builds maintainable systems.",
                List.of(new Profile.Contact("GitHub", "https://github.com/dev-yunseong")),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
