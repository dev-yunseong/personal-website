package dev.yunseong.website.manage.domain;

import java.util.Locale;

/**
 * Device class, browser, and operating system read out of a User-Agent string.
 *
 * <p>Deliberately a finite, ordered rule set rather than a User-Agent database:
 * it only has to answer "roughly how much traffic is mobile, Chrome, Android",
 * and anything it does not recognise lands in {@link #UNKNOWN}. Runs at query
 * time over distinct User-Agent values, never on the request path.
 *
 * <p>Rule order carries the meaning: iPad reports {@code Mobile}, iOS reports
 * {@code like Mac OS X}, Android reports {@code Linux}, and every Chromium
 * browser reports both {@code Chrome} and {@code Safari}, so the narrower rule
 * has to be checked first.
 */
public record UserAgentProfile(String deviceClass, String browser, String operatingSystem) {

    public static final String UNKNOWN = "Unknown";

    private static final UserAgentProfile UNRECOGNISED = new UserAgentProfile(UNKNOWN, UNKNOWN, UNKNOWN);

    public static UserAgentProfile of(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return UNRECOGNISED;
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);
        return new UserAgentProfile(deviceClassOf(ua), browserOf(ua), operatingSystemOf(ua));
    }

    private static String deviceClassOf(String ua) {
        if (ua.contains("ipad") || ua.contains("tablet") || (ua.contains("android") && !ua.contains("mobile"))) {
            return "Tablet";
        }
        if (ua.contains("mobile") || ua.contains("iphone") || ua.contains("ipod") || ua.contains("android")) {
            return "Mobile";
        }
        if (ua.contains("windows") || ua.contains("macintosh") || ua.contains("mac os x")
                || ua.contains("cros") || ua.contains("linux") || ua.contains("x11")) {
            return "Desktop";
        }
        return UNKNOWN;
    }

    private static String browserOf(String ua) {
        if (ua.contains("edg")) {
            // Edg/, EdgA/, EdgiOS/ — Chromium Edge never spells out "edge".
            return "Edge";
        }
        if (ua.contains("opr/") || ua.contains("opera")) {
            return "Opera";
        }
        if (ua.contains("samsungbrowser")) {
            return "Samsung Internet";
        }
        if (ua.contains("firefox") || ua.contains("fxios")) {
            return "Firefox";
        }
        if (ua.contains("chrome") || ua.contains("crios") || ua.contains("chromium")) {
            return "Chrome";
        }
        if (ua.contains("safari")) {
            return "Safari";
        }
        if (ua.contains("msie") || ua.contains("trident")) {
            return "Internet Explorer";
        }
        return UNKNOWN;
    }

    private static String operatingSystemOf(String ua) {
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ipod")) {
            return "iOS";
        }
        if (ua.contains("android")) {
            return "Android";
        }
        if (ua.contains("windows")) {
            return "Windows";
        }
        if (ua.contains("cros")) {
            return "ChromeOS";
        }
        if (ua.contains("mac os x") || ua.contains("macintosh")) {
            return "macOS";
        }
        if (ua.contains("linux") || ua.contains("x11")) {
            return "Linux";
        }
        return UNKNOWN;
    }
}
