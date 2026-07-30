package dev.yunseong.website.manage.domain;

/** A single traffic-source row for the console: a channel label or an external domain. */
public record RefererStat(String label, long requestCount) {
}
