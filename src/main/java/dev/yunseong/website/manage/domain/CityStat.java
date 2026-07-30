package dev.yunseong.website.manage.domain;

/** Request count at an approximate city location. */
public record CityStat(
        Long cityId,
        String cityName,
        Double latitude,
        Double longitude,
        Integer accuracyRadiusKm,
        long requestCount,
        long botRequestCount
) {
}
