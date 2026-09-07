package dev.yunseong.website.manage.domain;

import java.time.LocalDateTime;

/**
 * Read model for the admin console's request history. The JPA entity is not
 * the API contract; this flattens {@link RequestStatistics} together with the
 * autonomous system, which is resolved from the stored IP at read time
 * because it is not itself a stored column.
 */
public record RequestDetail(
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
        String countryCode,
        String cityName,
        Double latitude,
        Double longitude,
        Integer accuracyRadiusKm,
        Long asnNumber,
        String asnOrganisation
) {
    public static RequestDetail of(RequestStatistics row, AutonomousSystem network) {
        return new RequestDetail(
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
                row.getCountryCode(),
                row.getCityName(),
                row.getLatitude(),
                row.getLongitude(),
                row.getAccuracyRadiusKm(),
                network == null ? null : network.number(),
                network == null ? null : network.organisation());
    }
}
