package dev.yunseong.website.manage.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserAgentProfileTest {

    @Test
    void of_WithDesktopBrowsers_ReadsBrowserAndOperatingSystem() {
        assertProfile("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Desktop", "Chrome", "Windows");
        assertProfile("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
                "Desktop", "Firefox", "Windows");
        assertProfile("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.2210.91",
                "Desktop", "Edge", "Windows");
        assertProfile("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15",
                "Desktop", "Safari", "macOS");
        assertProfile("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Desktop", "Chrome", "macOS");
        assertProfile("Mozilla/5.0 (X11; Linux x86_64; rv:121.0) Gecko/20100101 Firefox/121.0",
                "Desktop", "Firefox", "Linux");
        assertProfile("Mozilla/5.0 (X11; CrOS x86_64 14541.0.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Desktop", "Chrome", "ChromeOS");
        assertProfile("Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 6.2; Trident/6.0)",
                "Desktop", "Internet Explorer", "Windows");
    }

    @Test
    void of_WithIosUserAgents_SeparatesPhoneFromTablet() {
        // iOS reports "like Mac OS X" and the iPad reports "Mobile", so rule order decides both answers.
        assertProfile("Mozilla/5.0 (iPhone; CPU iPhone OS 17_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Mobile/15E148 Safari/604.1",
                "Mobile", "Safari", "iOS");
        assertProfile("Mozilla/5.0 (iPhone; CPU iPhone OS 17_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/120.0.6099.119 Mobile/15E148 Safari/604.1",
                "Mobile", "Chrome", "iOS");
        assertProfile("Mozilla/5.0 (iPad; CPU OS 17_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Mobile/15E148 Safari/604.1",
                "Tablet", "Safari", "iOS");
    }

    @Test
    void of_WithAndroidUserAgents_SeparatesPhoneFromTablet() {
        // Android reports "Linux", and only phones add the "Mobile" token.
        assertProfile("Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
                "Mobile", "Chrome", "Android");
        assertProfile("Mozilla/5.0 (Linux; Android 13; SM-X710) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Tablet", "Chrome", "Android");
        assertProfile("Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/23.0 Chrome/115.0.0.0 Mobile Safari/537.36",
                "Mobile", "Samsung Internet", "Android");
    }

    @Test
    void of_WithMissingOrUnrecognisedUserAgent_FallsBackToUnknown() {
        assertProfile(null, "Unknown", "Unknown", "Unknown");
        assertProfile("", "Unknown", "Unknown", "Unknown");
        assertProfile("   ", "Unknown", "Unknown", "Unknown");
        assertProfile("some-internal-client/1.2", "Unknown", "Unknown", "Unknown");
        assertProfile("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)",
                "Unknown", "Unknown", "Unknown");
    }

    private static void assertProfile(String userAgent, String deviceClass, String browser, String operatingSystem) {
        UserAgentProfile profile = UserAgentProfile.of(userAgent);
        assertEquals(deviceClass, profile.deviceClass(), "device class of " + userAgent);
        assertEquals(browser, profile.browser(), "browser of " + userAgent);
        assertEquals(operatingSystem, profile.operatingSystem(), "operating system of " + userAgent);
    }
}
