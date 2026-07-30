package dev.yunseong.website.manage.domain;

import java.time.LocalDateTime;

/**
 * One 404 URI aggregated over a period. The referer counts split the occurrences
 * into links from our own host (broken internal links we can fix), links from
 * other hosts, and direct hits with no referer.
 */
public record NotFoundStat(
        String uri,
        long occurrences,
        LocalDateTime firstSeen,
        LocalDateTime lastSeen,
        String sampleReferer,
        long internalRefererCount,
        long externalRefererCount,
        long directCount
) {
}
