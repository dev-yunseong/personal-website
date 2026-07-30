package dev.yunseong.website.manage.domain;

import dev.yunseong.website.manage.domain.RefererSource.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RefererSourceTest {

    private static final String INTERNAL_HOST = "yunseong.dev";

    @ParameterizedTest
    @CsvSource({
            "https://www.google.com/search?q=yunseong, www.google.com",
            "http://example.com, example.com",
            "https://example.com:8443/path, example.com",
            "example.com/path/to/page, example.com",
            "example.com, example.com",
            "HTTPS://Example.COM/Path, example.com",
            "https://user:pw@example.com/path, example.com",
            "https://example.com?q=1, example.com",
            "https://example.com#frag, example.com",
            "'  https://example.com/path  ', example.com",
            "https://127.0.0.1:8080/, 127.0.0.1",
    })
    void hostOf_ExtractsHost(String referer, String expectedHost) {
        assertEquals(expectedHost, RefererSource.hostOf(referer));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            "",
            "   ",
            "https://",
            "not a url",
            "://missing-scheme-name",
            "javascript:void(0)",
            "https://[::1]:8080/",
    })
    void hostOf_WithNoUsableHost_ReturnsNull(String referer) {
        assertNull(RefererSource.hostOf(referer));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://www.google.com/search?q=x",
            "https://google.co.kr/",
            "https://search.naver.com/search.naver",
            "https://www.bing.com/",
            "https://duckduckgo.com/",
    })
    void of_WithSearchEngineReferer_ClassifiesAsSearch(String referer) {
        assertEquals(Channel.SEARCH, RefererSource.of(referer, INTERNAL_HOST).channel());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://x.com/someone/status/1",
            "https://t.co/abcdef",
            "https://twitter.com/someone",
            "https://old.reddit.com/r/java",
            "https://news.ycombinator.com/item?id=1",
            "https://www.linkedin.com/feed/",
            "https://m.facebook.com/",
    })
    void of_WithSocialReferer_ClassifiesAsSocial(String referer) {
        assertEquals(Channel.SOCIAL, RefererSource.of(referer, INTERNAL_HOST).channel());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://yunseong.dev/public/memos",
            "https://www.yunseong.dev/",
            "yunseong.dev/public/projects",
    })
    void of_WithOwnHostReferer_ClassifiesAsInternal(String referer) {
        assertEquals(Channel.INTERNAL, RefererSource.of(referer, INTERNAL_HOST).channel());
    }

    @Test
    void of_WithUnknownExternalHost_ClassifiesAsReferral() {
        RefererSource source = RefererSource.of("https://blog.example.com/post", INTERNAL_HOST);

        assertEquals(Channel.REFERRAL, source.channel());
        assertEquals("blog.example.com", source.host());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "not a url"})
    void of_WithoutUsableReferer_ClassifiesAsDirect(String referer) {
        RefererSource source = RefererSource.of(referer, INTERNAL_HOST);

        assertEquals(Channel.DIRECT, source.channel());
        assertNull(source.host());
    }

    @Test
    void of_WithoutConfiguredInternalHost_TreatsOwnHostAsReferral() {
        assertEquals(Channel.REFERRAL, RefererSource.of("https://yunseong.dev/", null).channel());
    }

    @Test
    void of_WithSingleLetterBrandLabelOnAnotherDomain_DoesNotClassifyAsSocial() {
        // "x" and "t" are matched as whole hosts, so unrelated subdomains stay referral.
        assertEquals(Channel.REFERRAL, RefererSource.of("https://x.example.com/", INTERNAL_HOST).channel());
        assertEquals(Channel.REFERRAL, RefererSource.of("https://t.example.com/", INTERNAL_HOST).channel());
    }

    @Test
    void channelIsExternal_OnlyForHostBearingChannels() {
        assertEquals(
                java.util.List.of(Channel.SEARCH, Channel.SOCIAL, Channel.REFERRAL),
                java.util.Arrays.stream(Channel.values()).filter(Channel::isExternal).toList());
    }
}
