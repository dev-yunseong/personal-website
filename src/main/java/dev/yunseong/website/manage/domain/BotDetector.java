package dev.yunseong.website.manage.domain;

import java.util.regex.Pattern;

/**
 * Classifies a request as bot or human traffic from its User-Agent.
 *
 * <p>Runs on the request path, so the decision must stay at regex cost: one
 * precompiled case-insensitive substring match, no lookups or external calls.
 * A missing or blank User-Agent is treated as a bot because real browsers
 * always send the header.
 */
public final class BotDetector {

    private static final Pattern BOT_USER_AGENT = Pattern.compile(
            "bot|crawl|spider|slurp|curl|wget|python-requests|httpx|scrapy"
                    + "|okhttp|java/|go-http-client|libwww-perl|headlesschrome"
                    + "|facebookexternalhit|feedfetcher|monitoring|uptime",
            Pattern.CASE_INSENSITIVE);

    private BotDetector() {
    }

    public static boolean isBot(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return true;
        }
        return BOT_USER_AGENT.matcher(userAgent).find();
    }
}
