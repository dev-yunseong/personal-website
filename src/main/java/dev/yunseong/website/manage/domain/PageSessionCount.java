package dev.yunseong.website.manage.domain;

/**
 * How many sessions started or ended at a page. Unlike {@link UriStat} the count
 * is sessions, not requests.
 */
public record PageSessionCount(String uri, long sessionCount) {
}
