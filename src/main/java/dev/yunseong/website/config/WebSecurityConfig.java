package dev.yunseong.website.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers(
                                "/",
                                "/error",
                                "/robots.txt",
                                "/google59d6c60daa0cb654.html",
                                "/favicon.ico",
                                "/sitemap.xml",
                                "/public/**")
                        .permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(withDefaults());;

        return http.build();
    }
}