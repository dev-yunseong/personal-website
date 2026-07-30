package dev.yunseong.website.manage.domain;

/** Request count grouped by ISO 3166-1 alpha-2 country code. */
public record CountryStat(String countryCode, long requestCount) {
}
