package dev.yunseong.website.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class RunnerTokenFilter extends OncePerRequestFilter {
    public static final String TOKEN_HEADER = "X-Runner-Token";
    private final String configuredToken;

    public RunnerTokenFilter(String configuredToken) { this.configuredToken = configuredToken; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String presented = request.getHeader(TOKEN_HEADER);
        if (configuredToken == null || configuredToken.isBlank() || presented == null || !MessageDigest.isEqual(
                configuredToken.getBytes(StandardCharsets.UTF_8), presented.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(HttpStatus.UNAUTHORIZED.value());
            return;
        }
        chain.doFilter(request, response);
    }
}
