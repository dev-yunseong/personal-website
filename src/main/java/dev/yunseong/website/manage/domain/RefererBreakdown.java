package dev.yunseong.website.manage.domain;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Traffic-source report for one period: the share of every channel plus one page
 * of the external domains behind them.
 */
public record RefererBreakdown(List<RefererStat> channels, Page<RefererStat> domains) {
}
