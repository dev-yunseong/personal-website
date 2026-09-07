package dev.yunseong.website.manage.domain;

/**
 * The header shape of one request, limited to what bot classification reads.
 *
 * <p>A plain value object on purpose: {@link BotDetector} owns every rule, and
 * this type owns nothing but the evidence, so the scoring model can be read in
 * one place. The web layer fills it in, which is also the only layer that has
 * the raw headers.
 *
 * <p>Every field is nullable. A null means the client did not send the header,
 * which is itself the signal — no caller may substitute a placeholder.
 *
 * @param userAgent      {@code User-Agent}
 * @param accept         {@code Accept}
 * @param acceptLanguage {@code Accept-Language}
 * @param secFetchSite   {@code Sec-Fetch-Site}
 * @param secFetchMode   {@code Sec-Fetch-Mode}
 * @param secFetchDest   {@code Sec-Fetch-Dest}
 * @param secChUa        {@code Sec-CH-UA}, sent by Chromium in secure contexts only
 */
public record RequestFingerprint(
        String userAgent,
        String accept,
        String acceptLanguage,
        String secFetchSite,
        String secFetchMode,
        String secFetchDest,
        String secChUa) {

    /**
     * A request whose User-Agent is the only thing known about the client.
     *
     * <p>Scores as bot-shaped, because a real browser never sends a bare
     * {@code User-Agent} and nothing else.
     */
    public static RequestFingerprint ofUserAgent(String userAgent) {
        return new RequestFingerprint(userAgent, null, null, null, null, null, null);
    }
}
