package dev.yunseong.website.manage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The production runtime has no ASN database on developer machines or in the
 * test runtime, so what matters here is that its absence is harmless: every
 * lookup answers null and nothing throws.
 */
class AsnResolverTest {

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "/nonexistent/GeoLite2-ASN.mmdb"})
    void withoutAUsableDatabaseTheResolverStaysUnavailable(String path) {
        AsnResolver resolver = new AsnResolver(path);

        assertThat(resolver.isAvailable()).isFalse();
        assertThat(resolver.resolve("203.0.113.42")).isNull();
    }

    @Test
    void aNullPathIsTreatedAsNoDatabase() {
        AsnResolver resolver = new AsnResolver(null);

        assertThat(resolver.isAvailable()).isFalse();
        assertThat(resolver.resolve("203.0.113.42")).isNull();
    }

    @Test
    void aHostNameNeverReachesTheDatabase() {
        // The IP-literal guard is what keeps a forged CF-Connecting-IP from
        // turning into a DNS lookup on the request path.
        AsnResolver resolver = new AsnResolver("");

        assertThat(resolver.resolve("example.com")).isNull();
        assertThat(resolver.resolve(null)).isNull();
    }
}
