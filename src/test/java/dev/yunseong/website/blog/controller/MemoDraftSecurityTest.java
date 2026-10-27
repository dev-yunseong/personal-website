package dev.yunseong.website.blog.controller;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.service.MemoDraftService;
import dev.yunseong.website.blog.service.MemoNavigationService;
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
    MockMvc mvc;

    @BeforeEach
    void setup() {
        reset(drafts);
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
            AgentMemoDraftController.class, ConsoleDraftController.class})
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
    }
}
