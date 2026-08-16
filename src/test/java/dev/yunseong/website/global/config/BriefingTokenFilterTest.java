package dev.yunseong.website.global.config;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class BriefingTokenFilterTest {

    private static final String TOKEN = "s3cr3t-briefing-token";

    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final MockFilterChain chain = new MockFilterChain();

    private static MockHttpServletRequest request(String presentedToken) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/agent/briefings/news");
        if (presentedToken != null) {
            request.addHeader(BriefingTokenFilter.TOKEN_HEADER, presentedToken);
        }
        return request;
    }

    private boolean passedThrough() {
        return chain.getRequest() != null;
    }

    @Test
    void passesTheRequestThroughWhenTheTokenMatches() throws Exception {
        new BriefingTokenFilter(TOKEN).doFilter(request(TOKEN), response, chain);

        assertThat(passedThrough()).isTrue();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    void rejectsAMissingToken() throws Exception {
        new BriefingTokenFilter(TOKEN).doFilter(request(null), response, chain);

        assertThat(passedThrough()).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getContentAsString()).contains(BriefingTokenFilter.TOKEN_HEADER);
    }

    @Test
    void rejectsAWrongToken() throws Exception {
        new BriefingTokenFilter(TOKEN).doFilter(request("wrong"), response, chain);

        assertThat(passedThrough()).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void rejectsATokenThatOnlySharesAPrefix() throws Exception {
        new BriefingTokenFilter(TOKEN).doFilter(request(TOKEN.substring(0, 5)), response, chain);

        assertThat(passedThrough()).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * Forgetting to set BRIEFING_TOKEN must close the endpoint, not open it. Otherwise a
     * fresh environment would ship an unauthenticated writer.
     */
    @Test
    void failsClosedWhenNoTokenIsConfigured() throws Exception {
        new BriefingTokenFilter("").doFilter(request("anything"), response, chain);

        assertThat(passedThrough()).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void failsClosedWhenTheConfiguredTokenIsNull() throws Exception {
        new BriefingTokenFilter(null).doFilter(request("anything"), response, chain);

        assertThat(passedThrough()).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void failsClosedWhenBothTheConfiguredAndPresentedTokenAreBlank() throws Exception {
        new BriefingTokenFilter("   ").doFilter(request("   "), response, chain);

        assertThat(passedThrough()).isFalse();
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * A valid token authorises publishing briefings and nothing else. Populating the
     * security context would also switch off the memo visibility filter for the request,
     * turning a write token into read access over every private memo.
     */
    @Test
    void doesNotAuthenticateThePrincipal() throws Exception {
        SecurityContextHolder.clearContext();

        new BriefingTokenFilter(TOKEN).doFilter(request(TOKEN), response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
