package dev.yunseong.website.blog.controller;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.service.MemoDraftService;
import dev.yunseong.website.blog.service.MemoNavigationService;
import dev.yunseong.website.blog.service.MemoLineDiff;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.context.IContext;
import dev.yunseong.website.blog.service.S3StorageService;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.dao.DataIntegrityViolationException;
import dev.yunseong.website.global.config.AgentApiSecurityConfig;
import dev.yunseong.website.global.config.DraftApiSecurityConfig;
import dev.yunseong.website.global.config.WebSecurityConfig;
import dev.yunseong.website.manage.controller.ConsoleDraftController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = MemoDraftSecurityTest.Config.class)
@TestPropertySource(properties = {"app.draft.token=draft-secret", "app.briefing.token=briefing-secret"})
class MemoDraftSecurityTest {
    @Autowired WebApplicationContext context;
    @Autowired MemoDraftService drafts;
    @Autowired SpringTemplateEngine templates;
    @Autowired S3StorageService storage;
    MockMvc mvc;

    @BeforeEach
    void setup() {
        reset(drafts, templates, storage);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void onlyDraftTokenCanSubmitWithoutCsrf() throws Exception {
        mvc.perform(post("/api/agent/drafts").contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/agent/drafts").header("X-Briefing-Token", "briefing-secret"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/agent/drafts").header("X-Draft-Token", "wrong"))
                .andExpect(status().isUnauthorized());
        when(drafts.submit("notes/a", "한글\n")).thenReturn(new Memo(1L, "/private/draft/notes/a", "한글\n", null, null));
        mvc.perform(post("/api/agent/drafts").header("X-Draft-Token", "draft-secret")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED).param("target", "notes/a").param("content", "한글\n"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("/private/draft/notes/a"));
        verify(drafts).submit("notes/a", "한글\n");
    }

    @Test
    void tokenCannotReviewApproveOrPublishBriefings() throws Exception {
        mvc.perform(get("/admin/console/drafts/1").header("X-Draft-Token", "draft-secret"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(put("/admin/console/drafts/1/approval").with(csrf()).header("X-Draft-Token", "draft-secret")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"revision\":\"r\"}"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/api/agent/briefings/news").header("X-Draft-Token", "draft-secret"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(drafts);
    }

    @Test
    void approvalRequiresAdminSessionAndCsrf() throws Exception {
        mvc.perform(put("/admin/console/drafts/1/approval").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"revision\":\"r\"}"))
                .andExpect(status().isForbidden());
        when(drafts.approve(1, "r")).thenReturn(42L);
        mvc.perform(put("/admin/console/drafts/1/approval").with(user("admin")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"revision\":\"r\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.url").value("/public/memos/42"));
    }

    @Test
    void renameRequiresTokenAndOnlyPassesPaths() throws Exception {
        for (String token : new String[]{"", "wrong"}) {
            mvc.perform(post("/api/agent/drafts/rename").header("X-Draft-Token", token)
                            .param("source", "notes/a").param("target", "notes/b"))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(drafts);
        when(drafts.rename("notes/a", "notes/b")).thenReturn(new Memo(7L, "/private/draft/notes/b", "body", null, null));
        mvc.perform(post("/api/agent/drafts/rename").header("X-Draft-Token", "draft-secret")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("source", "notes/a").param("target", "notes/b").param("content", "ignored"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("/private/draft/notes/b"))
                .andExpect(jsonPath("$.reviewUrl").value("/admin/console/drafts/7"));
        verify(drafts).rename("notes/a", "notes/b");
        verifyNoMoreInteractions(drafts);
    }

    @Test
    void concurrentDestinationConflictReturns409() throws Exception {
        when(drafts.rename("notes/a", "notes/b")).thenThrow(new DataIntegrityViolationException("duplicate"));
        mvc.perform(post("/api/agent/drafts/rename").header("X-Draft-Token", "draft-secret")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("source", "notes/a").param("target", "notes/b"))
                .andExpect(status().isConflict());
    }

    @Test
    void imageUploadRequiresDraftTokenAndReturnsStorageUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", new byte[]{1, 2, 3});
        for (String token : new String[]{"", "wrong"}) {
            mvc.perform(multipart("/api/agent/drafts/images").file(file).header("X-Draft-Token", token))
                    .andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(storage);
        when(storage.uploadFile(any())).thenReturn("https://storage.example/website/image.png");
        mvc.perform(multipart("/api/agent/drafts/images").file(file).header("X-Draft-Token", "draft-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://storage.example/website/image.png"))
                .andExpect(jsonPath("$.markdown").value("![image](https://storage.example/website/image.png)"));
        verify(storage).uploadFile(any());
        verifyNoInteractions(drafts);
    }

    @Test
    void imageUploadRejectsNonImagesEmptyFilesAndMissingFile() throws Exception {
        for (MockMultipartFile file : new MockMultipartFile[]{
                new MockMultipartFile("file", "file.txt", "text/plain", new byte[]{1}),
                new MockMultipartFile("file", "image.svg", "image/svg+xml", new byte[]{1}),
                new MockMultipartFile("file", "image.png", "image/png", new byte[0]),
                new MockMultipartFile("file", "image.png", null, new byte[]{1})}) {
            mvc.perform(multipart("/api/agent/drafts/images").file(file).header("X-Draft-Token", "draft-secret"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(multipart("/api/agent/drafts/images").header("X-Draft-Token", "draft-secret"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(storage);
    }

    @Test
    void imageValidationFailureDoesNotExposeStorageDetails() throws Exception {
        when(storage.uploadFile(any())).thenThrow(new java.io.IOException("invalid pixels"));
        mvc.perform(multipart("/api/agent/drafts/images")
                        .file(new MockMultipartFile("file", "fake.png", "image/png", new byte[]{1}))
                        .header("X-Draft-Token", "draft-secret"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectionRequiresSessionAndCsrf() throws Exception {
        mvc.perform(delete("/admin/console/drafts/1").with(csrf()).header("X-Draft-Token", "draft-secret")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"revision\":\"r\"}"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(delete("/admin/console/drafts/1").with(user("admin"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"revision\":\"r\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(drafts);
        mvc.perform(delete("/admin/console/drafts/1").with(user("admin")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"revision\":\"r\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.url").value("/admin/console/drafts"));
        verify(drafts).reject(1, "r");
    }

    @Configuration
    @EnableWebMvc
    @Import({WebSecurityConfig.class, AgentApiSecurityConfig.class, DraftApiSecurityConfig.class,
            AgentMemoDraftController.class, AgentDraftImageController.class, ConsoleDraftController.class})
    static class Config {
        @org.springframework.context.annotation.Bean
        org.springframework.security.core.userdetails.UserDetailsService users() {
            return new org.springframework.security.provisioning.InMemoryUserDetailsManager(
                    org.springframework.security.core.userdetails.User.withUsername("admin")
                            .password("{noop}admin").roles("USER").build());
        }

        @org.springframework.context.annotation.Bean
        org.springframework.security.web.authentication.rememberme.PersistentTokenRepository loginTokens() {
            return org.mockito.Mockito.mock(org.springframework.security.web.authentication.rememberme.PersistentTokenRepository.class);
        }

        @Bean MemoDraftService drafts() { return mock(MemoDraftService.class); }
        @Bean MemoNavigationService navigation() { return mock(MemoNavigationService.class); }
        @Bean SpringTemplateEngine templates() { return mock(SpringTemplateEngine.class); }
        @Bean S3StorageService storage() { return mock(S3StorageService.class); }
    }

    @Test
    void authenticatedReviewBuildsPreviewFromSameDraftWithoutWriting() throws Exception {
        Memo draft = new Memo(2L, "/private/draft/notes/a", "# 초안", null, null);
        var review = new MemoDraftService.Review(draft, "/notes/a", null, "revision",
                MemoLineDiff.compare("", draft.getContent()));
        when(drafts.review(2)).thenReturn(review);
        when(templates.process(eq("console/draft_preview"), any(IContext.class))).thenReturn("preview-document");
        mvc.perform(get("/admin/console/drafts/2").with(user("admin")))
                .andExpect(status().isOk()).andExpect(view().name("console/draft_review"))
                .andExpect(model().attribute("review", review))
                .andExpect(model().attribute("previewHtml", "preview-document"));
        verify(drafts).review(2);
        verifyNoMoreInteractions(drafts);
        verify(templates).process(eq("console/draft_preview"), argThat(c -> c.getVariable("draft") == draft));
    }
}
