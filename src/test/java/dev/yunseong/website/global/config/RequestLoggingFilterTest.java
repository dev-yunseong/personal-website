package dev.yunseong.website.global.config;

import dev.yunseong.website.global.util.ClientIpResolver;
import dev.yunseong.website.manage.service.RequestStatisticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RequestLoggingFilterTest {

    private static final String EDGE_IP = "172.71.18.5";

    @Mock
    private RequestStatisticsService requestStatisticsService;

    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final MockFilterChain chain = new MockFilterChain();

    private static MockHttpServletRequest request(String cloudflareHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setRemoteAddr(EDGE_IP);
        if (cloudflareHeader != null) {
            request.addHeader(ClientIpResolver.CLOUDFLARE_HEADER, cloudflareHeader);
        }
        return request;
    }

    private String recordedIp() {
        ArgumentCaptor<String> ip = ArgumentCaptor.forClass(String.class);
        verify(requestStatisticsService).recordRequest(
                eq("/"), eq("GET"), any(), any(), ip.capture(), any(), anyInt());
        return ip.getValue();
    }

    @Test
    void recordsTheVisitorAddressCloudflareForwards() throws Exception {
        new RequestLoggingFilter(requestStatisticsService).doFilter(request("203.0.113.42"), response, chain);

        assertThat(recordedIp()).isEqualTo("203.0.113.42");
    }

    @Test
    void recordsTheSocketAddressWhenNoProxyHeaderIsPresent() throws Exception {
        new RequestLoggingFilter(requestStatisticsService).doFilter(request(null), response, chain);

        assertThat(recordedIp()).isEqualTo(EDGE_IP);
    }

    @Test
    void passesTheRequestDownTheChain() throws Exception {
        new RequestLoggingFilter(requestStatisticsService).doFilter(request("203.0.113.42"), response, chain);

        assertThat(chain.getRequest()).isNotNull();
    }
}
