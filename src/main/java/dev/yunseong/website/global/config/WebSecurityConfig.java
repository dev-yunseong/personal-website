package dev.yunseong.website.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import static org.springframework.security.config.Customizer.withDefaults;

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
            "/login",
            "/error",
            "/sitemap.xml",
            "/robots.txt",
            // static resources under src/main/resources/static
            "/favicon.ico",
            "/css/**",
            "/js/**",
            "/images/**",
            // search console site-verification files; add new ones here
            "/google59d6c60daa0cb654.html",
            "/naver99ee1f05e669de1b60d5746cbf6f988f.html"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        // stated explicitly so that widening
                        // management.endpoints.web.exposure never publishes an
                        // operational endpoint anonymously by accident
                        .requestMatchers("/actuator/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(withDefaults())
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll());

        return http.build();
    }
}