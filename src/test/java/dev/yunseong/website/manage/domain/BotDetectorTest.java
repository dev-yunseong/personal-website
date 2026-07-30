package dev.yunseong.website.manage.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotDetectorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)",
            "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko); compatible; GPTBot/1.2; +https://openai.com/gptbot",
            "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko); compatible; ClaudeBot/1.0; +claudebot@anthropic.com",
            "Mozilla/5.0 (compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm)",
            "curl/8.4.0",
            "Wget/1.21.4",
            "python-requests/2.31.0",
    })
    void isBot_WithCrawlerOrToolUserAgent_ReturnsTrue(String userAgent) {
        assertTrue(BotDetector.isBot(userAgent));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
    })
    void isBot_WithBrowserUserAgent_ReturnsFalse(String userAgent) {
        assertFalse(BotDetector.isBot(userAgent));
    }

    @Test
    void isBot_IgnoresCase() {
        assertTrue(BotDetector.isBot("GOOGLEBOT"));
        assertTrue(BotDetector.isBot("CURL/8.4.0"));
    }

    @Test
    void isBot_WithMissingUserAgent_ReturnsTrue() {
        // Real browsers always send the header, so its absence means a scripted client.
        assertTrue(BotDetector.isBot(null));
        assertTrue(BotDetector.isBot(""));
        assertTrue(BotDetector.isBot("   "));
    }
}
