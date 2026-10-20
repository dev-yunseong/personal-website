package dev.yunseong.website.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Configuration
public class DraftApiSecurityConfig {
    @Bean
    @Order(0)
    public SecurityFilterChain draftApiSecurityFilterChain(HttpSecurity http,
            @Value("${app.draft.token:}") String token) throws Exception {
        return http.securityMatcher("/api/agent/drafts", "/api/agent/drafts/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .anonymous(anonymous -> anonymous.disable())
                .addFilterBefore(new DraftTokenFilter(token), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll()).build();
    }

    // Deliberately does not establish an admin principal or permit access to private memos.
    private static class DraftTokenFilter extends OncePerRequestFilter {
        private final String token;

        private DraftTokenFilter(String token) { this.token = token; }

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                FilterChain chain) throws ServletException, IOException {
            String presented = request.getHeader("X-Draft-Token");
            if (token.isBlank() || presented == null || !MessageDigest.isEqual(
                    token.getBytes(StandardCharsets.UTF_8), presented.getBytes(StandardCharsets.UTF_8))) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            chain.doFilter(request, response);
        }
    }
}
