package dev.yunseong.website.manage.controller;

import dev.yunseong.website.blog.service.S3StorageService;
import dev.yunseong.website.manage.domain.RequestStatistics;
import dev.yunseong.website.manage.repository.RequestStatisticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.S3Client;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Drives the two new pages through the real dispatcher and the real Thymeleaf
 * engine, which the fragment-level template tests cannot do: the layout
 * decoration, the security rule and the 404 only exist end to end.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class RequestBrowseIntegrationTest {

    @MockitoBean
    private S3Client s3Client;

    @MockitoBean
    private S3StorageService s3StorageService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestStatisticsRepository requestStatisticsRepository;

    private Long storedId;

    @BeforeEach
    void seed() {
        requestStatisticsRepository.deleteAll();
        storedId = requestStatisticsRepository.saveAndFlush(new RequestStatistics(
                "/public/memos", "GET", "https://news.example", "curl/8.0", "1.1.1.1",
                404, false, 12, "KR")).getId();
    }

    @Test
    void requestList_WithoutAuthentication_IsNotServed() throws Exception {
        mockMvc.perform(get("/admin/console/requests"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void requestDetail_WithoutAuthentication_IsNotServed() throws Exception {
        mockMvc.perform(get("/admin/console/requests/" + storedId))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser
    void requestList_RendersThroughTheSiteLayout() throws Exception {
        mockMvc.perform(get("/admin/console/requests").param("uri", "/public/memos"))
                .andExpect(status().isOk())
                .andExpect(view().name("console/requests"))
                .andExpect(content().string(containsString("/css/console-request.css")))
                .andExpect(content().string(containsString("/public/memos")))
                // Decorated by layout.html, so the site chrome is present.
                .andExpect(content().string(containsString("<!DOCTYPE html>")))
                .andExpect(content().string(containsString("site-main")));
    }

    @Test
    @WithMockUser
    void requestList_WithAFilterThatMatchesNothing_RendersTheEmptyState() throws Exception {
        mockMvc.perform(get("/admin/console/requests").param("ip", "9.9.9.9"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("조건에 맞는 요청이 없습니다")));
    }

    @Test
    @WithMockUser
    void requestDetail_RendersEveryStoredFieldThroughTheSiteLayout() throws Exception {
        mockMvc.perform(get("/admin/console/requests/" + storedId))
                .andExpect(status().isOk())
                .andExpect(view().name("console/request_detail"))
                .andExpect(model().attributeExists("detail"))
                .andExpect(content().string(containsString("https://news.example")))
                .andExpect(content().string(containsString("curl/8.0")))
                .andExpect(content().string(containsString("1.1.1.1")))
                .andExpect(content().string(containsString("site-main")));
    }

    @Test
    @WithMockUser
    void requestDetail_WithUnknownId_Returns404() throws Exception {
        mockMvc.perform(get("/admin/console/requests/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void from_ThatLeavesTheConsole_NeverReachesTheReturnLink() throws Exception {
        mockMvc.perform(get("/admin/console/requests/" + storedId)
                        .param("from", "https://evil.example/steal"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("returnHref", "/admin/console?tab=history"))
                .andExpect(content().string(
                        not(containsString("evil.example"))));
    }
}
