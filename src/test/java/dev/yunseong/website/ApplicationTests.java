package dev.yunseong.website;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class ApplicationTests {

    /**
     * Boots the whole application context. Held apart from the slice tests
     * because it is the only check that every bean definition still fits
     * together — a wiring break shows up here before it shows up in production.
     *
     * <p>Carries no property block of its own: everything it needs to boot
     * without a local .env comes from {@code application-test.yml}.
     */
    @Test
    void contextLoads() {
    }
}
