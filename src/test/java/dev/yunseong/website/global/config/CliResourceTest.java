package dev.yunseong.website.global.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {WebSecurityConfig.class, CliResourceTest.ResourceConfig.class})
@WebAppConfiguration
class CliResourceTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void downloadableCommandMatchesCanonicalSourceBytes() throws Exception {
        mockMvc.perform(get("/cli/website"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(Files.readAllBytes(Path.of("bin/website"))));
    }

    @Test
    void installerIsDownloadableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/cli/install.sh"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(Files.readAllBytes(
                        Path.of("src/main/resources/static/cli/install.sh"))));
    }

    @Test
    void downloadableSkillMatchesCanonicalSourceBytes() throws Exception {
        mockMvc.perform(get("/cli/website-project-report/SKILL.md"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(Files.readAllBytes(Path.of("skills/website-project-report/SKILL.md"))));
    }

    @Test
    void downloadableExampleMatchesCanonicalSourceBytes() throws Exception {
        mockMvc.perform(get("/cli/project-report.json"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(Files.readAllBytes(Path.of("docs/examples/project-report.json"))));
    }

    @Configuration
    @EnableWebMvc
    static class ResourceConfig implements WebMvcConfigurer {

        @Bean
        UserDetailsService users() {
            return new InMemoryUserDetailsManager();
        }

        @Bean
        PersistentTokenRepository loginTokens() {
            return org.mockito.Mockito.mock(PersistentTokenRepository.class);
        }

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/cli/**")
                    .addResourceLocations("classpath:/static/cli/");
        }
    }
}
