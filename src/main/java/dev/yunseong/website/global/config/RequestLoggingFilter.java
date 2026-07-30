package dev.yunseong.website.global.config;

import dev.yunseong.website.manage.service.RequestStatisticsService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestLoggingFilter implements Filter {
    private final RequestStatisticsService requestStatisticsService;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest httpRequest) {
            long startTime = System.currentTimeMillis();
            log.info("[Request Start] method={}, uri={}, ip={}, userAgent={}",
                    httpRequest.getMethod(),
                    httpRequest.getRequestURI(),
                    httpRequest.getRemoteAddr(),
                    Objects.requireNonNullElse(httpRequest.getHeader(HttpHeaders.USER_AGENT), "Unknown")
            );
            try {
                filterChain.doFilter(servletRequest, servletResponse);
            } finally {
                long duration = System.currentTimeMillis() - startTime;
                Integer statusCode = (servletResponse instanceof HttpServletResponse httpResponse)
                        ? httpResponse.getStatus() : null;

                log.info("[Request End] method={}, uri={}, status={}, duration={}ms",
                        httpRequest.getMethod(),
                        httpRequest.getRequestURI(),
                        statusCode,
                        duration
                );

                // Record statistics for public URLs
                String referer = httpRequest.getHeader(HttpHeaders.REFERER);
                String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
                String ipAddress = httpRequest.getRemoteAddr();
                requestStatisticsService.recordRequest(httpRequest.getRequestURI(), httpRequest.getMethod(), referer, userAgent, ipAddress, statusCode, (int) duration);
            }
        } else {
            log.warn("Received a non-HTTP request. Skipping request logging.");
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }
}
