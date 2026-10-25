package dev.yunseong.website.global.config;

import dev.yunseong.website.global.service.PersistentLoginService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.UUID;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    /**
     * Fail-closed whitelist: anything not listed here requires authentication.
     * A new endpoint is private by default; make it public by adding it here.
     */
    private static final String[] PUBLIC_PATHS = {
            "/",
            "/public/**",
            "/api/public/**",
            // the briefing digest is public even though the memos behind it are not
            "/briefing",
            "/login",
            "/error",
            "/sitemap.xml",
            "/robots.txt",
            // static resources under src/main/resources/static
            "/favicon.ico",
            "/apple-touch-icon.png",
            "/css/**",
            "/js/**",
            "/images/**",
            // search console site-verification files; add new ones here
            "/google59d6c60daa0cb654.html",
            "/naver99ee1f05e669de1b60d5746cbf6f988f.html"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, PersistentTokenRepository loginTokens,
                                                   UserDetailsService users) throws Exception {
        PersistentLoginService persistentLogin = new PersistentLoginService(UUID.randomUUID().toString(), users, loginTokens);
        http
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/admin/console")
                        .permitAll())
                .rememberMe(remember -> remember
                        .rememberMeServices(persistentLogin)
                        .key(persistentLogin.getKey()))
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll());

        return http.build();
    }
}