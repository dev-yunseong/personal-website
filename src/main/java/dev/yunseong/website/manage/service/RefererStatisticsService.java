package dev.yunseong.website.manage.service;

import dev.yunseong.website.manage.domain.RefererBreakdown;
import dev.yunseong.website.manage.domain.RefererGroupCount;
import dev.yunseong.website.manage.domain.RefererSource;
import dev.yunseong.website.manage.domain.RefererSource.Channel;
import dev.yunseong.website.manage.domain.RefererStat;
import dev.yunseong.website.manage.repository.RefererStatisticsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the referer column into traffic-source figures.
 *
 * <p>The database groups by referer; this service only folds those groups into
 * hosts and channels, because host parsing needs the tested Java parser rather
 * than portable SQL.
 */
@Service
public class RefererStatisticsService {
    private static final int PAGE_SIZE = 10;

    private final RefererStatisticsRepository refererStatisticsRepository;
    private final String internalHost;

    public RefererStatisticsService(RefererStatisticsRepository refererStatisticsRepository,
                                    @Value("${app.base-url:}") String baseUrl) {
        this.refererStatisticsRepository = refererStatisticsRepository;
        this.internalHost = RefererSource.hostOf(baseUrl);
    }

    @Transactional(readOnly = true)
    public RefererBreakdown getRefererBreakdownForLastDays(int days, boolean excludeBots, int domainPage) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<RefererGroupCount> groups = refererStatisticsRepository.countByRefererAndBot(startDate);

        Map<Channel, Long> channelCounts = new EnumMap<>(Channel.class);
        Map<String, Long> domainCounts = new HashMap<>();
        for (RefererGroupCount group : groups) {
            if (excludeBots && group.bot()) {
                continue;
            }
            RefererSource source = RefererSource.of(group.referer(), internalHost);
            channelCounts.merge(source.channel(), group.requestCount(), Long::sum);
            if (source.channel().isExternal()) {
                domainCounts.merge(source.host(), group.requestCount(), Long::sum);
            }
        }

        return new RefererBreakdown(channelStats(channelCounts), pageOfDomains(domainCounts, domainPage));
    }

    /** Every channel is reported, zeros included, so the direct bucket stays visible. */
    private static List<RefererStat> channelStats(Map<Channel, Long> channelCounts) {
        List<RefererStat> stats = new ArrayList<>(Channel.values().length);
        for (Channel channel : Channel.values()) {
            stats.add(new RefererStat(channel.label(), channelCounts.getOrDefault(channel, 0L)));
        }
        return stats;
    }

    private static PageImpl<RefererStat> pageOfDomains(Map<String, Long> domainCounts, int domainPage) {
        Comparator<RefererStat> byCountDesc = Comparator.comparingLong(RefererStat::requestCount);
        List<RefererStat> domains = domainCounts.entrySet().stream()
                .map(entry -> new RefererStat(entry.getKey(), entry.getValue()))
                .sorted(byCountDesc.reversed().thenComparing(RefererStat::label))
                .toList();

        Pageable pageable = PageRequest.of(Math.max(0, domainPage), PAGE_SIZE);
        int from = Math.min((int) pageable.getOffset(), domains.size());
        int to = Math.min(from + PAGE_SIZE, domains.size());
        return new PageImpl<>(domains.subList(from, to), pageable, domains.size());
    }
}
