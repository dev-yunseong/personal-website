package dev.yunseong.website.manage.domain;

/** Approximate MaxMind location for an IP address. Every field may be absent. */
public record GeoLocation(
        String countryCode,
        Long cityGeoNameId,
        String cityName,
        Double latitude,
        Double longitude,
        Integer accuracyRadiusKm
) {
}
