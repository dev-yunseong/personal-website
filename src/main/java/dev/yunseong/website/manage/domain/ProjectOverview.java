package dev.yunseong.website.manage.domain;

import java.time.Instant;

public record ProjectOverview(String slug, ProjectReportContent content, Instant updatedAt,
                              boolean stale) {}
