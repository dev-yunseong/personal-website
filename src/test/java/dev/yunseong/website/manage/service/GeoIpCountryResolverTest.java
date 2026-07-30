package dev.yunseong.website.manage.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The graceful fallback matters more than the lookup itself: no test runtime and
 * no developer machine has the licensed GeoLite2 database.
 */
class GeoIpCountryResolverTest {

    @Test
    void withUnconfiguredPath_IsUnavailableAndResolvesToNull() {
        GeoIpCountryResolver resolver = assertDoesNotThrow(() -> new GeoIpCountryResolver(""));

        assertFalse(resolver.isAvailable());
        assertNull(resolver.resolveCountryCode("1.1.1.1"));
    }

    @Test
    void withMissingFile_IsUnavailableAndResolvesToNull() {
        GeoIpCountryResolver resolver = assertDoesNotThrow(
                () -> new GeoIpCountryResolver("/nonexistent/GeoLite2-Country.mmdb"));

        assertFalse(resolver.isAvailable());
        assertNull(resolver.resolveCountryCode("8.8.8.8"));
        assertNull(resolver.resolveCountryCode(null));
    }

    @Test
    void withCorruptFile_IsUnavailableAndDoesNotThrow(@TempDir Path tempDir) throws IOException {
        Path notADatabase = Files.writeString(tempDir.resolve("GeoLite2-Country.mmdb"), "not an mmdb");

        GeoIpCountryResolver resolver = assertDoesNotThrow(
                () -> new GeoIpCountryResolver(notADatabase.toString()));

        assertFalse(resolver.isAvailable());
        assertNull(resolver.resolveCountryCode("1.1.1.1"));
    }

    @Test
    void isIpLiteral_AcceptsAddressesAndRejectsHostNames() {
        assertTrue(GeoIpCountryResolver.isIpLiteral("8.8.8.8"));
        assertTrue(GeoIpCountryResolver.isIpLiteral("203.0.113.42"));
        assertTrue(GeoIpCountryResolver.isIpLiteral("2001:db8::1"));
        assertTrue(GeoIpCountryResolver.isIpLiteral("::ffff:1.2.3.4"));

        // A spoofed X-Forwarded-For must never reach InetAddress and cause a DNS lookup.
        assertFalse(GeoIpCountryResolver.isIpLiteral("attacker.example.com"));
        assertFalse(GeoIpCountryResolver.isIpLiteral("localhost"));
        assertFalse(GeoIpCountryResolver.isIpLiteral("deadbeef"));
        assertFalse(GeoIpCountryResolver.isIpLiteral("1.2.3.4.example.com"));
        assertFalse(GeoIpCountryResolver.isIpLiteral(""));
        assertFalse(GeoIpCountryResolver.isIpLiteral(null));
    }
}
