package dev.yunseong.website.global.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pins the access policy route by route.
 * <p>
 * Only {@link WebSecurityConfig} is loaded; a catch-all stub controller stands in
 * for every real handler and static resource, so a 200 means "the authorization
 * rules let an anonymous request through" and nothing else. All probes use GET
 * because the authorization rules are method-agnostic.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {WebSecurityConfig.class, WebSecurityConfigTest.StubMvcConfig.class})
@WebAppConfiguration
class WebSecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/",
            "/public/memos",
            "/public/memos/1",
            "/public/projects",
            "/public/projects/1",
            "/public/apps",
            "/public/apps/1",
            "/public/search",
            "/public/chat",
            "/api/public/chat",
            "/api/public/search/suggestions",
            "/sitemap.xml",
            "/robots.txt",
            "/error",
            "/login",
            "/favicon.ico",
            "/css/site.css",
            "/js/chat.js",
            "/images/profile-placeholder.svg",
            "/google59d6c60daa0cb654.html",
            "/naver99ee1f05e669de1b60d5746cbf6f988f.html"
    })
    void publicPaths_areReachableAnonymously(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/admin/console",
            "/admin/chat",
            "/admin/memos",
            "/admin/memos/new",
            "/admin/memos/edit/1",
            "/admin/memos/profile/validate",
            "/admin/memos/preview",
            "/admin/miniapps",
            "/admin/miniapps/new",
            "/admin/miniapps/edit/1",
            "/admin/upload/file",
            "/api/admin/miniapps/generate",
            "/api/admin/console/summary",
            "/api/admin/console/distribution",
            "/api/admin/console/visitors",
            "/api/admin/console/referer",
            "/api/admin/console/not-found",
            "/api/admin/console/content",
            "/api/admin/console/geo/countries"
    })
    void adminPaths_redirectAnonymousToLogin(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator", "/actuator/health", "/actuator/metrics"})
    void actuatorPaths_redirectAnonymousToLogin(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    /** The point of the whitelist: an unlisted path is private, not public. */
    @ParameterizedTest
    @ValueSource(strings = {"/api/admins/typo", "/admins/console", "/whatever/new/endpoint"})
    void unlistedPaths_defaultToAuthenticated(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void logout_staysAnonymouslyReachable() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(redirectedUrl("/"));
    }

    @Configuration
    @EnableWebMvc
    @RestController
    static class StubMvcConfig {

        @RequestMapping("/**")
        String any() {
            return "ok";
        }
    }
}
