package dev.yunseong.website.manage.domain;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The traffic source a stored {@code Referer} header points at: its host and the
 * channel that host belongs to.
 *
 * <p>Host extraction happens here rather than in SQL because JPQL has no regex
 * and stored referers include scheme-less values, which {@link java.net.URI}
 * parses as a path instead of an authority.
 *
 * @param host    lowercase host, or {@code null} when the referer carries none
 * @param channel the channel the host belongs to
 */
public record RefererSource(String host, Channel channel) {

    /**
     * Traffic channel. {@link Channel#DIRECT} covers a missing or empty referer
     * and, rarely, one with no parseable host.
     */
    public enum Channel {
        SEARCH("Search"),
        SOCIAL("Social & Community"),
        REFERRAL("Other Referral"),
        INTERNAL("Internal"),
        DIRECT("Direct");

        private final String label;

        Channel(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        /** Whether requests in this channel came from a host worth listing by domain. */
        public boolean isExternal() {
            return this == SEARCH || this == SOCIAL || this == REFERRAL;
        }
    }

    /** Host labels matched anywhere in the host, so ccTLD variants such as {@code google.co.kr} count. */
    private static final Set<String> SEARCH_LABELS = Set.of("google", "naver", "bing", "duckduckgo");

    private static final Set<String> SOCIAL_LABELS =
            Set.of("twitter", "reddit", "ycombinator", "linkedin", "facebook");

    /**
     * Single-letter brands are matched on the whole host: a bare {@code x} or
     * {@code t} label would also match unrelated subdomains.
     */
    private static final Set<String> SOCIAL_HOSTS = Set.of("x.com", "t.co");

    /**
     * A usable host needs at least one dot, which rejects junk referers such as
     * {@code javascript:void(0)} along with dotless intranet names.
     */
    private static final Pattern PLAUSIBLE_HOST = Pattern.compile("[a-z0-9-]+(\\.[a-z0-9-]+)+");

    public static RefererSource of(String referer, String internalHost) {
        String host = hostOf(referer);
        if (host == null) {
            return new RefererSource(null, Channel.DIRECT);
        }
        return new RefererSource(host, channelOf(host, internalHost));
    }

    /**
     * Extracts the lowercase host from a referer, tolerating a missing scheme, a
     * port, userinfo, and outright garbage.
     *
     * @return the host, or {@code null} when the value carries no plausible host
     */
    public static String hostOf(String referer) {
        if (referer == null) {
            return null;
        }
        String value = referer.trim();
        int schemeEnd = value.indexOf("://");
        if (schemeEnd >= 0) {
            value = value.substring(schemeEnd + 3);
        }
        String authority = value.split("[/?#]", 2)[0];
        int userInfoEnd = authority.lastIndexOf('@');
        if (userInfoEnd >= 0) {
            authority = authority.substring(userInfoEnd + 1);
        }
        int portStart = authority.indexOf(':');
        if (portStart >= 0) {
            authority = authority.substring(0, portStart);
        }
        String host = authority.toLowerCase(Locale.ROOT);
        return PLAUSIBLE_HOST.matcher(host).matches() ? host : null;
    }

    private static Channel channelOf(String host, String internalHost) {
        if (internalHost != null && (host.equals(internalHost) || host.endsWith("." + internalHost))) {
            return Channel.INTERNAL;
        }
        if (SOCIAL_HOSTS.contains(host)) {
            return Channel.SOCIAL;
        }
        for (String label : host.split("\\.")) {
            if (SEARCH_LABELS.contains(label)) {
                return Channel.SEARCH;
            }
            if (SOCIAL_LABELS.contains(label)) {
                return Channel.SOCIAL;
            }
        }
        return Channel.REFERRAL;
    }
}
