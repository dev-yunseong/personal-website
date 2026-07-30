package dev.yunseong.website.manage.service;

import dev.yunseong.website.manage.domain.TrafficDistribution;
import dev.yunseong.website.manage.domain.TrafficSlice;
import dev.yunseong.website.manage.domain.UriStat;
import dev.yunseong.website.manage.domain.UserAgentProfile;
import dev.yunseong.website.manage.repository.TrafficDistributionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TrafficDistributionService {

    private static final int DAYS_PER_WEEK = 7;
    private static final int HOURS_PER_DAY = 24;

    private final TrafficDistributionRepository trafficDistributionRepository;

    /**
     * Day-of-week by hour grid plus device/browser/OS shares for the period.
     *
     * <p>{@code created_at} is a zone-less timestamp that Hibernate writes from a
     * {@code LocalDateTime} and reads back unconverted, so its value is wall-clock
     * time in the server zone. The buckets keep that basis and the response
     * reports the zone, which is the only reading that stays true whatever zone
     * the runtime is configured with.
     */
    @Transactional(readOnly = true)
    public TrafficDistribution getDistributionForLastDays(int days, boolean includeBots) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);

        long[][] hourOfWeek = new long[DAYS_PER_WEEK][HOURS_PER_DAY];
        for (Object[] row : trafficDistributionRepository.countByHour(startDate, includeBots)) {
            LocalDateTime hourBucket = (LocalDateTime) row[0];
            int dayIndex = hourBucket.getDayOfWeek().getValue() - 1;
            hourOfWeek[dayIndex][hourBucket.getHour()] += ((Number) row[1]).longValue();
        }

        Map<String, Long> devices = new HashMap<>();
        Map<String, Long> browsers = new HashMap<>();
        Map<String, Long> operatingSystems = new HashMap<>();
        for (UriStat userAgentCount : trafficDistributionRepository.countByUserAgent(startDate, includeBots)) {
            UserAgentProfile profile = UserAgentProfile.of(userAgentCount.uri());
            long count = userAgentCount.requestCount();
            devices.merge(profile.deviceClass(), count, Long::sum);
            browsers.merge(profile.browser(), count, Long::sum);
            operatingSystems.merge(profile.operatingSystem(), count, Long::sum);
        }

        return new TrafficDistribution(
                ZoneId.systemDefault().getId(),
                toCountRows(hourOfWeek),
                toSlicesByCountDesc(devices),
                toSlicesByCountDesc(browsers),
                toSlicesByCountDesc(operatingSystems));
    }

    private static List<List<Long>> toCountRows(long[][] hourOfWeek) {
        return Arrays.stream(hourOfWeek)
                .map(row -> Arrays.stream(row).boxed().toList())
                .toList();
    }

    private static List<TrafficSlice> toSlicesByCountDesc(Map<String, Long> counts) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> new TrafficSlice(entry.getKey(), entry.getValue()))
                .toList();
    }
}
