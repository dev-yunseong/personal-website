package dev.yunseong.website.global.config;

import dev.yunseong.website.global.util.ClientIpResolver;
import dev.yunseong.website.manage.domain.RequestFingerprint;
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
    /**
     * Fetch metadata headers, absent from {@link HttpHeaders} because they are
     * set by the browser and never by an application.
     */
    private static final String SEC_FETCH_SITE = "Sec-Fetch-Site";
    private static final String SEC_FETCH_MODE = "Sec-Fetch-Mode";
    private static final String SEC_FETCH_DEST = "Sec-Fetch-Dest";
    private static final String SEC_CH_UA = "Sec-CH-UA";

    private final RequestStatisticsService requestStatisticsService;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest httpRequest) {
            long startTime = System.currentTimeMillis();
            String ipAddress = ClientIpResolver.resolve(httpRequest);
            log.info("[Request Start] method={}, uri={}, ip={}, userAgent={}",
                    httpRequest.getMethod(),
                    httpRequest.getRequestURI(),
                    ipAddress,
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
                requestStatisticsService.recordRequest(httpRequest.getRequestURI(), httpRequest.getMethod(), referer,
                        fingerprintOf(httpRequest), ipAddress, statusCode, (int) duration);
            }
        } else {
            log.warn("Received a non-HTTP request. Skipping request logging.");
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    /**
     * Headers are read here rather than in the detector because this filter is
     * the only layer that still holds the raw request. Missing headers stay
     * null: their absence is the evidence bot classification weighs.
     */
    private static RequestFingerprint fingerprintOf(HttpServletRequest request) {
        return new RequestFingerprint(
                request.getHeader(HttpHeaders.USER_AGENT),
                request.getHeader(HttpHeaders.ACCEPT),
                request.getHeader(HttpHeaders.ACCEPT_LANGUAGE),
                request.getHeader(SEC_FETCH_SITE),
                request.getHeader(SEC_FETCH_MODE),
                request.getHeader(SEC_FETCH_DEST),
                request.getHeader(SEC_CH_UA));
    }
}
