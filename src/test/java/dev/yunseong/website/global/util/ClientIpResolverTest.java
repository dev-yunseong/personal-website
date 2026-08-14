package dev.yunseong.website.global.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private static final String EDGE_IP = "172.71.18.5";

    private static MockHttpServletRequest request(String cloudflareHeader) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        request.setRemoteAddr(EDGE_IP);
        if (cloudflareHeader != null) {
            request.addHeader(ClientIpResolver.CLOUDFLARE_HEADER, cloudflareHeader);
        }
        return request;
    }

    @Test
    void prefersTheVisitorAddressCloudflareForwards() {
        assertThat(ClientIpResolver.resolve(request("203.0.113.42"))).isEqualTo("203.0.113.42");
    }

    @Test
    void keepsIpv6VisitorsIntact() {
        assertThat(ClientIpResolver.resolve(request("2001:db8::1"))).isEqualTo("2001:db8::1");
        assertThat(ClientIpResolver.resolve(request("::ffff:1.2.3.4"))).isEqualTo("::ffff:1.2.3.4");
    }

    @Test
    void fallsBackToTheSocketAddressWithoutTheHeader() {
        assertThat(ClientIpResolver.resolve(request(null))).isEqualTo(EDGE_IP);
    }

    @Test
    void fallsBackWhenTheHeaderIsBlank() {
        assertThat(ClientIpResolver.resolve(request(""))).isEqualTo(EDGE_IP);
        assertThat(ClientIpResolver.resolve(request("   "))).isEqualTo(EDGE_IP);
    }

    @Test
    void fallsBackWhenTheHeaderIsNotAnAddress() {
        // A forged header must not reach the ip column or the GeoIP lookup.
        assertThat(ClientIpResolver.resolve(request("attacker.example.com"))).isEqualTo(EDGE_IP);
        assertThat(ClientIpResolver.resolve(request("203.0.113.42, 198.51.100.7"))).isEqualTo(EDGE_IP);
        assertThat(ClientIpResolver.resolve(request("x".repeat(500)))).isEqualTo(EDGE_IP);
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertThat(ClientIpResolver.resolve(request(" 203.0.113.42 "))).isEqualTo("203.0.113.42");
    }
}
