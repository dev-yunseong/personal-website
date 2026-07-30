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
class GeoIpLocationResolverTest {

    @Test
    void withUnconfiguredPath_IsUnavailableAndResolvesToNull() {
        GeoIpLocationResolver resolver = assertDoesNotThrow(() -> new GeoIpLocationResolver(""));

        assertFalse(resolver.isAvailable());
        assertNull(resolver.resolve("1.1.1.1"));
    }

    @Test
    void withMissingFile_IsUnavailableAndResolvesToNull() {
        GeoIpLocationResolver resolver = assertDoesNotThrow(
                () -> new GeoIpLocationResolver("/nonexistent/GeoLite2-City.mmdb"));

        assertFalse(resolver.isAvailable());
        assertNull(resolver.resolve("8.8.8.8"));
        assertNull(resolver.resolve(null));
    }

    @Test
    void withCorruptFile_IsUnavailableAndDoesNotThrow(@TempDir Path tempDir) throws IOException {
        Path notADatabase = Files.writeString(tempDir.resolve("GeoLite2-City.mmdb"), "not an mmdb");

        GeoIpLocationResolver resolver = assertDoesNotThrow(
                () -> new GeoIpLocationResolver(notADatabase.toString()));

        assertFalse(resolver.isAvailable());
        assertNull(resolver.resolve("1.1.1.1"));
    }

    @Test
    void isIpLiteral_AcceptsAddressesAndRejectsHostNames() {
        assertTrue(GeoIpLocationResolver.isIpLiteral("8.8.8.8"));
        assertTrue(GeoIpLocationResolver.isIpLiteral("203.0.113.42"));
        assertTrue(GeoIpLocationResolver.isIpLiteral("2001:db8::1"));
        assertTrue(GeoIpLocationResolver.isIpLiteral("::ffff:1.2.3.4"));

        // A spoofed X-Forwarded-For must never reach InetAddress and cause a DNS lookup.
        assertFalse(GeoIpLocationResolver.isIpLiteral("attacker.example.com"));
        assertFalse(GeoIpLocationResolver.isIpLiteral("localhost"));
        assertFalse(GeoIpLocationResolver.isIpLiteral("deadbeef"));
        assertFalse(GeoIpLocationResolver.isIpLiteral("1.2.3.4.example.com"));
        assertFalse(GeoIpLocationResolver.isIpLiteral(""));
        assertFalse(GeoIpLocationResolver.isIpLiteral(null));
    }
}
