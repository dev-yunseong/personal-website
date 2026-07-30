package dev.yunseong.website.manage.domain;

import java.time.LocalDateTime;

/** Geo-filtered request row that deliberately excludes IP and User-Agent. */
public record GeoRequestStat(
        String uri,
        Integer statusCode,
        boolean bot,
        String cityName,
        LocalDateTime createdAt
) {
}
