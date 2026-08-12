package dev.yunseong.website.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security for the machine-to-machine intake API under {@code /api/agent/**}.
 *
 * <p>A chain of its own, ahead of {@link WebSecurityConfig}, because an agent posting
 * from a cron job needs the opposite of what a browser session needs: no CSRF token to
 * fetch first, no session to keep, no login form to be redirected to. Folding those
 * exemptions into the site-wide chain would relax them for the whole site.
 *
 * <p>Authorisation is {@link BriefingTokenFilter}, not Spring Security's authentication.
 * The chain itself permits everything it matches — the filter has already rejected any
 * request without a valid token by then.
 */
@Configuration
public class AgentApiSecurityConfig {

    public static final String AGENT_API_PATTERN = "/api/agent/**";

    @Value("${app.briefing.token:}")
    private String briefingToken;

    @Bean
    @Order(1)
    public SecurityFilterChain agentApiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(AGENT_API_PATTERN)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .logout(logout -> logout.disable())
                .anonymous(anonymous -> anonymous.disable())
                .addFilterBefore(new BriefingTokenFilter(briefingToken),
                        UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll());

        return http.build();
    }
}
