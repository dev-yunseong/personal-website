package dev.yunseong.website.manage.domain;

import java.util.List;

/**
 * Visitor and session metrics for one period.
 *
 * <p>Every figure is an approximation: a visitor is the pair
 * {@code (ip, user_agent)}, so users sharing a NAT collapse into one visitor and a
 * mobile user whose IP rotates splits into several. Rates are fractions in
 * {@code [0, 1]}.
 */
public record VisitorMetrics(
        long uniqueVisitors,
        long sessions,
        long pageViews,
        double pagesPerSession,
        double singlePageSessionRate,
        long medianSessionSeconds,
        double returningVisitorRate,
        List<PageSessionCount> topEntryPages,
        List<PageSessionCount> topExitPages) {
}
