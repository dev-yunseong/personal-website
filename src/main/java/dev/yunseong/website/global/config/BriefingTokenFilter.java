package dev.yunseong.website.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Gates the agent intake API on a shared secret sent in {@code X-Briefing-Token}.
 *
 * <p>A dedicated token rather than the admin account: the agent publishes from a cron
 * job on another host, so its credential lives in that host's environment. Keeping it
 * separate means a leak costs one env var to rotate and never reaches {@code /admin/**},
 * which the admin password would.
 *
 * <p>The filter never populates the {@link org.springframework.security.core.context.SecurityContext}.
 * A valid token authorises publishing briefings and nothing else; granting an
 * authenticated principal would also switch off
 * {@link dev.yunseong.website.blog.aspect.ContentVisibilityAspect} for the request and
 * quietly widen the token into read access over every private memo.
 */
public class BriefingTokenFilter extends OncePerRequestFilter {

    public static final String TOKEN_HEADER = "X-Briefing-Token";

    private final String configuredToken;

    public BriefingTokenFilter(String configuredToken) {
        this.configuredToken = configuredToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!isAuthorized(request.getHeader(TOKEN_HEADER))) {
            reject(response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Fails closed: with no token configured the API is shut rather than open, so
     * forgetting to set {@code BRIEFING_TOKEN} in an environment cannot turn the intake
     * endpoint into an unauthenticated writer.
     */
    private boolean isAuthorized(String presentedToken) {
        if (configuredToken == null || configuredToken.isBlank() || presentedToken == null) {
            return false;
        }
        return MessageDigest.isEqual(
                configuredToken.getBytes(StandardCharsets.UTF_8),
                presentedToken.getBytes(StandardCharsets.UTF_8));
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("Missing or invalid " + TOKEN_HEADER + ".\n");
    }
}
