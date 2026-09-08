package dev.yunseong.website.manage.domain;

import java.time.LocalDateTime;

/**
 * Read model for a row of the request list. Carries what a scan line shows and
 * nothing more; {@link RequestDetail} adds the autonomous system and the city
 * and coordinate fields the detail page needs.
 *
 * <p>The ASN is deliberately absent: resolving it costs one mmdb lookup per
 * row, and no column of the list renders it.
 */
public record RequestSummary(
        Long id,
        LocalDateTime createdAt,
        String method,
        String uri,
        Integer statusCode,
        Integer durationMs,
        String ip,
        String referer,
        String userAgent,
        boolean bot,
        Integer botScore,
        String botSignals,
        String countryCode
) {
    public static RequestSummary of(RequestStatistics row) {
        return new RequestSummary(
                row.getId(),
                row.getCreatedAt(),
                row.getMethod(),
                row.getUri(),
                row.getStatusCode(),
                row.getDurationMs(),
                row.getIp(),
                row.getReferer(),
                row.getUserAgent(),
                row.isBot(),
                row.getBotScore(),
                row.getBotSignals(),
                row.getCountryCode());
    }
}
