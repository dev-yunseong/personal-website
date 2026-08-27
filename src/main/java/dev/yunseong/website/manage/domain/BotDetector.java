package dev.yunseong.website.manage.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Classifies a request as bot or human traffic from the headers it carries.
 *
 * <p>Runs on the request path, so the decision must stay at regex cost: two
 * precompiled case-insensitive matches over the User-Agent plus blank checks on
 * a handful of headers. No lookups, no external calls.
 *
 * <p>Two kinds of evidence, deliberately separated. A client that declares
 * itself — a crawler token in the User-Agent, or no User-Agent at all — is a
 * bot outright. Everything else is weighed: each signal below is something a
 * real browser always does and a scripted client usually skips, and the request
 * counts as a bot only once the weights reach {@link #BOT_SCORE_THRESHOLD}.
 * Summing rather than OR-ing is what makes a spoofed User-Agent detectable
 * without a single quirk of an old browser being enough to convict it.
 *
 * <p>The weights are constants rather than configuration: the score is stored
 * on every row, so tuning is done by reading the recorded distribution and
 * changing these numbers, not by an operator toggling production behaviour.
 */
public final class BotDetector {

    /** Above any additive total, so declared bots sort above inferred ones. */
    public static final int CERTAIN_BOT_SCORE = 9;

    private static final Pattern BOT_USER_AGENT = Pattern.compile(
            "bot|crawl|spider|slurp|curl|wget|python-requests|httpx|scrapy"
                    + "|okhttp|java/|go-http-client|libwww-perl|headlesschrome"
                    + "|facebookexternalhit|feedfetcher|monitoring|uptime",
            Pattern.CASE_INSENSITIVE);

    /**
     * Chromium desktop and Android tokens. iOS Chrome and iOS Edge are excluded
     * on purpose — they spell themselves {@code CriOS} and {@code EdgiOS}, run
     * on WebKit, and send no client hints, so matching them would convict every
     * iPhone.
     */
    private static final Pattern CLAIMS_CHROMIUM = Pattern.compile(
            "chrome/|chromium/|edg/", Pattern.CASE_INSENSITIVE);

    /**
     * A User-Agent claiming Chromium while sending no {@code Sec-CH-UA} was
     * rewritten: Chromium has sent client hints since version 89 and cannot be
     * made to drop them. Weighted below the threshold on its own because an
     * embedded WebView on an old system image can miss them too.
     */
    private static final int REWRITTEN_USER_AGENT_SCORE = 2;

    /**
     * {@code Sec-Fetch-*} ships in Chrome 76+, Firefox 90+, and Safari 16.4+,
     * so a browser released after 2023 always sends all three. Anything older
     * sends none, which is why this alone does not convict.
     */
    private static final int NO_SEC_FETCH_SCORE = 2;

    private static final int NO_ACCEPT_LANGUAGE_SCORE = 1;
    private static final int NO_ACCEPT_SCORE = 1;

    /**
     * Visitors browse from consumer ISPs and mobile carriers; scrapers run on
     * rented machines. Weighted lowest of all signals because the inference is
     * about the network rather than the client, and a person really can arrive
     * from a datacenter through a VPN.
     */
    private static final int DATACENTER_NETWORK_SCORE = 1;

    /**
     * Autonomous system organisations that rent out compute. Matched against the
     * MaxMind organisation string, which is free text, so these are fragments
     * rather than exact names; the short ambiguous ones are anchored.
     *
     * <p>Cloudflare, Akamai, and Fastly are left out on purpose. iCloud Private
     * Relay egresses through them, so every Safari user with it enabled would
     * pick up this signal — and paired with an older Safari that sends no
     * {@code Sec-Fetch-*} that is enough to reach the threshold. They are edge
     * networks anyway, not somewhere a scraper rents a virtual machine.
     *
     * <p>Bare "google" is left out for the same reason: it would match Google
     * Fiber, a consumer ISP, alongside Google Cloud.
     *
     * <p>The list is meant to grow from observed traffic rather than from
     * guesswork — see the recorded {@code bot_signals}.
     */
    private static final Pattern HOSTING_ORGANISATION = Pattern.compile(
            "amazon|\\baws\\b|google cloud|google-cloud|microsoft|azure|digitalocean|digital ocean"
                    + "|linode|\\bovh\\b|hetzner|oracle|alibaba|aliyun|tencent|vultr|choopa|contabo"
                    + "|scaleway|\\bm247\\b|leaseweb|ionos|netcup|upcloud|rackspace",
            Pattern.CASE_INSENSITIVE);

    /**
     * Two independent signals, so no single browser quirk convicts a visitor:
     * the cheapest bot shape that reaches it is "no {@code Sec-Fetch-*} and no
     * {@code Accept-Language}", which no shipping browser produces.
     */
    private static final int BOT_SCORE_THRESHOLD = 3;

    private BotDetector() {
    }

    /**
     * @param network the autonomous system the request came from, or {@code null}
     *                when no ASN database is loaded or the address is unknown
     */
    public static BotVerdict classify(RequestFingerprint client, AutonomousSystem network) {
        // Real browsers always send the header, so its absence means a scripted client.
        if (isBlank(client.userAgent())) {
            return new BotVerdict(true, CERTAIN_BOT_SCORE, "no-user-agent");
        }
        if (BOT_USER_AGENT.matcher(client.userAgent()).find()) {
            return new BotVerdict(true, CERTAIN_BOT_SCORE, "user-agent-pattern");
        }

        int score = 0;
        List<String> signals = new ArrayList<>();
        if (CLAIMS_CHROMIUM.matcher(client.userAgent()).find() && isBlank(client.secChUa())) {
            score += REWRITTEN_USER_AGENT_SCORE;
            signals.add("no-client-hints");
        }
        if (isBlank(client.secFetchSite()) && isBlank(client.secFetchMode()) && isBlank(client.secFetchDest())) {
            score += NO_SEC_FETCH_SCORE;
            signals.add("no-sec-fetch");
        }
        if (isBlank(client.acceptLanguage())) {
            score += NO_ACCEPT_LANGUAGE_SCORE;
            signals.add("no-accept-language");
        }
        if (isBlank(client.accept())) {
            score += NO_ACCEPT_SCORE;
            signals.add("no-accept");
        }
        if (network != null && !isBlank(network.organisation())
                && HOSTING_ORGANISATION.matcher(network.organisation()).find()) {
            score += DATACENTER_NETWORK_SCORE;
            signals.add("datacenter-ip");
        }
        return new BotVerdict(score >= BOT_SCORE_THRESHOLD, score, String.join(",", signals));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
