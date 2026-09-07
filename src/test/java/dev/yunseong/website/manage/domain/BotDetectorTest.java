package dev.yunseong.website.manage.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotDetectorTest {

    private static final String CHROME =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
    private static final String SAFARI =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15";
    private static final String IPHONE_CHROME =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/131.0.0.0 Mobile/15E148 Safari/604.1";

    /** Classification with no ASN database loaded, which is most of these cases. */
    private static BotVerdict classify(RequestFingerprint client) {
        return BotDetector.classify(client, null);
    }

    private static Fingerprint browser(String userAgent) {
        return new Fingerprint(userAgent);
    }

    /**
     * Test fixture, not production API: the record has seven nullable headers
     * and each case here varies one or two of them. Every field starts absent,
     * because absence is what the detector weighs.
     */
    private static final class Fingerprint {
        private final String userAgent;
        private String accept;
        private String acceptLanguage;
        private String secFetchSite;
        private String secFetchMode;
        private String secFetchDest;
        private String secChUa;

        private Fingerprint(String userAgent) {
            this.userAgent = userAgent;
        }

        /** Everything a current browser sends except the Chromium-only client hints. */
        Fingerprint withFullBrowserHeaders() {
            this.accept = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
            this.acceptLanguage = "ko-KR,ko;q=0.9,en-US;q=0.8";
            this.secFetchSite = "none";
            this.secFetchMode = "navigate";
            this.secFetchDest = "document";
            return this;
        }

        Fingerprint accept(String accept) {
            this.accept = accept;
            return this;
        }

        Fingerprint acceptLanguage(String acceptLanguage) {
            this.acceptLanguage = acceptLanguage;
            return this;
        }

        Fingerprint secChUa(String secChUa) {
            this.secChUa = secChUa;
            return this;
        }

        RequestFingerprint build() {
            return new RequestFingerprint(userAgent, accept, acceptLanguage,
                    secFetchSite, secFetchMode, secFetchDest, secChUa);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)",
            "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko); compatible; GPTBot/1.2; +https://openai.com/gptbot",
            "Mozilla/5.0 AppleWebKit/537.36 (KHTML, like Gecko); compatible; ClaudeBot/1.0; +claudebot@anthropic.com",
            "Mozilla/5.0 (compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm)",
            "curl/8.4.0",
            "Wget/1.21.4",
            "python-requests/2.31.0",
            "GOOGLEBOT",
            "CURL/8.4.0",
    })
    void classify_WithCrawlerOrToolUserAgent_ConvictsRegardlessOfCase(String userAgent) {
        assertTrue(classify(browser(userAgent).withFullBrowserHeaders().build()).bot());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Mobile/15E148 Safari/604.1",
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
    })
    void classify_WithBrowserSendingItsUsualHeaders_Acquits(String userAgent) {
        assertFalse(classify(browser(userAgent)
                .withFullBrowserHeaders()
                .secChUa("\"Chromium\";v=\"131\"")
                .build()).bot());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void classify_WithBlankUserAgent_Convicts(String userAgent) {
        assertTrue(classify(browser(userAgent).withFullBrowserHeaders().build()).bot());
    }

    @Nested
    class DeclaredBots {

        @Test
        void aCrawlerTokenConvictsWhateverElseTheRequestCarries() {
            BotVerdict verdict = classify(browser("Mozilla/5.0 (compatible; Googlebot/2.1)")
                    .withFullBrowserHeaders()
                    .build());

            assertThat(verdict.bot()).isTrue();
            assertThat(verdict.score()).isEqualTo(BotDetector.CERTAIN_BOT_SCORE);
            assertThat(verdict.signals()).isEqualTo("user-agent-pattern");
        }

        @Test
        void aMissingUserAgentConvicts() {
            BotVerdict verdict = classify(browser(null).withFullBrowserHeaders().build());

            assertThat(verdict.bot()).isTrue();
            assertThat(verdict.score()).isEqualTo(BotDetector.CERTAIN_BOT_SCORE);
            assertThat(verdict.signals()).isEqualTo("no-user-agent");
        }
    }

    @Nested
    class RealBrowsers {

        @Test
        void chromeSendingClientHintsAndFetchMetadataScoresZero() {
            BotVerdict verdict = classify(browser(CHROME)
                    .withFullBrowserHeaders()
                    .secChUa("\"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"")
                    .build());

            assertThat(verdict.bot()).isFalse();
            assertThat(verdict.score()).isZero();
            assertThat(verdict.signals()).isEmpty();
        }

        @Test
        void safariScoresZeroWithoutClientHints() {
            // Client hints are Chromium-only, so their absence must not count here.
            BotVerdict verdict = classify(browser(SAFARI).withFullBrowserHeaders().build());

            assertThat(verdict.bot()).isFalse();
            assertThat(verdict.score()).isZero();
        }

        @Test
        void iosChromeScoresZeroWithoutClientHints() {
            // CriOS is WebKit underneath and sends none; matching it would convict every iPhone.
            BotVerdict verdict = classify(browser(IPHONE_CHROME).withFullBrowserHeaders().build());

            assertThat(verdict.bot()).isFalse();
            assertThat(verdict.score()).isZero();
        }

        @Test
        void aBrowserTooOldForFetchMetadataStaysUnderTheThreshold() {
            // Safari before 16.4 sends no Sec-Fetch-* at all. One quirk must not convict.
            BotVerdict verdict = classify(browser(SAFARI)
                    .accept("text/html,application/xhtml+xml")
                    .acceptLanguage("ko-KR,ko;q=0.9")
                    .build());

            assertThat(verdict.bot()).isFalse();
            assertThat(verdict.signals()).isEqualTo("no-sec-fetch");
        }
    }

    @Nested
    class RewrittenUserAgents {

        @Test
        void aUserAgentClaimingChromiumWithoutClientHintsIsWeighed() {
            BotVerdict verdict = classify(browser(CHROME).withFullBrowserHeaders().build());

            assertThat(verdict.signals()).isEqualTo("no-client-hints");
            // Alone it stays under the threshold: an old embedded WebView looks the same.
            assertThat(verdict.bot()).isFalse();
        }

        @Test
        void aSpoofedChromeSendingNoBrowserHeadersConvicts() {
            BotVerdict verdict = classify(browser(CHROME).build());

            assertThat(verdict.bot()).isTrue();
            assertThat(verdict.signals())
                    .isEqualTo("no-client-hints,no-sec-fetch,no-accept-language,no-accept");
        }
    }

    @Nested
    class ScriptedClients {

        @Test
        void aSpoofedSafariSendingNoAcceptLanguageNorFetchMetadataConvicts() {
            // The cheapest shape that reaches the threshold without any client-hint evidence.
            BotVerdict verdict = classify(browser(SAFARI).accept("*/*").build());

            assertThat(verdict.bot()).isTrue();
            assertThat(verdict.score()).isEqualTo(3);
            assertThat(verdict.signals()).isEqualTo("no-sec-fetch,no-accept-language");
        }

        @Test
        void aBlankHeaderCountsTheSameAsAMissingOne() {
            BotVerdict verdict = classify(browser(SAFARI)
                    .accept("   ")
                    .acceptLanguage("")
                    .build());

            assertThat(verdict.bot()).isTrue();
            assertThat(verdict.signals()).isEqualTo("no-sec-fetch,no-accept-language,no-accept");
        }
    }

    @Nested
    class DatacenterNetworks {

        private static final AutonomousSystem AWS = new AutonomousSystem(16509L, "AMAZON-02");
        private static final AutonomousSystem KOREA_TELECOM = new AutonomousSystem(4766L, "Korea Telecom");

        @Test
        void aBrowserRequestFromARentedMachineStaysUnderTheThreshold() {
            BotVerdict verdict = BotDetector.classify(browser(SAFARI).withFullBrowserHeaders().build(), AWS);

            assertThat(verdict.bot()).isFalse();
            assertThat(verdict.signals()).isEqualTo("datacenter-ip");
        }

        @Test
        void theNetworkTipsAnOtherwiseUndecidedRequestOver() {
            // Sends no Sec-Fetch-* and nothing else suspicious: 2, one short of
            // the threshold. Where it came from decides it.
            Fingerprint borderline = browser(SAFARI).accept("text/html").acceptLanguage("ko-KR");

            assertThat(BotDetector.classify(borderline.build(), KOREA_TELECOM).bot()).isFalse();

            BotVerdict fromHosting = BotDetector.classify(borderline.build(), AWS);
            assertThat(fromHosting.bot()).isTrue();
            assertThat(fromHosting.score()).isEqualTo(3);
            assertThat(fromHosting.signals()).isEqualTo("no-sec-fetch,datacenter-ip");
        }

        @Test
        void aConsumerIspAddsNothing() {
            BotVerdict verdict = BotDetector.classify(
                    browser(SAFARI).withFullBrowserHeaders().build(), KOREA_TELECOM);

            assertThat(verdict.score()).isZero();
            assertThat(verdict.signals()).isEmpty();
        }

        @Test
        void anAbsentAsnDatabaseLeavesEveryOtherSignalIntact() {
            BotVerdict verdict = BotDetector.classify(browser(CHROME).build(), null);

            assertThat(verdict.bot()).isTrue();
            assertThat(verdict.signals()).doesNotContain("datacenter-ip");
        }

        @Test
        void anUnnamedOrganisationAddsNothing() {
            BotVerdict verdict = BotDetector.classify(browser(SAFARI).withFullBrowserHeaders().build(),
                    new AutonomousSystem(64512L, null));

            assertThat(verdict.score()).isZero();
        }

        /**
         * iCloud Private Relay egresses through these, so matching them would
         * convict real Safari visitors.
         */
        @ParameterizedTest
        @ValueSource(strings = {"CLOUDFLARENET", "AKAMAI-AS", "FASTLY"})
        void edgeNetworksCarryingPrivateRelayAreNotTreatedAsHosting(String organisation) {
            BotVerdict verdict = BotDetector.classify(browser(SAFARI).withFullBrowserHeaders().build(),
                    new AutonomousSystem(13335L, organisation));

            assertThat(verdict.signals()).isEmpty();
        }

        /** Google Fiber is a consumer ISP; only the cloud arm counts. */
        @Test
        void googleFiberIsNotTreatedAsHosting() {
            BotVerdict fiber = BotDetector.classify(browser(SAFARI).withFullBrowserHeaders().build(),
                    new AutonomousSystem(16591L, "Google Fiber Inc."));
            BotVerdict cloud = BotDetector.classify(browser(SAFARI).withFullBrowserHeaders().build(),
                    new AutonomousSystem(396982L, "GOOGLE-CLOUD-PLATFORM"));

            assertThat(fiber.signals()).isEmpty();
            assertThat(cloud.signals()).isEqualTo("datacenter-ip");
        }
    }
}
