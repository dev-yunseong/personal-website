package dev.yunseong.website.manage.domain;

import java.time.LocalDateTime;

/**
 * Traffic for one piece of content over the selected period.
 *
 * <p>{@code memoId}, {@code title}, {@code category} and {@code publishedAt} are
 * {@code null} when the URI could not be resolved to a publicly visible memo —
 * deleted, private, or simply not a memo detail URI. Such a row keeps its
 * {@code uri} and {@code requestCount} so it never disappears from the aggregate.
 */
public record MemoTrafficStat(
        String uri,
        Long memoId,
        String title,
        String category,
        long requestCount,
        LocalDateTime publishedAt,
        boolean evergreen
) {
    public boolean resolved() {
        return memoId != null;
    }
}
