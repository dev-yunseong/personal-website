package dev.yunseong.website.manage.service;

import dev.yunseong.website.manage.domain.CountryStat;
import dev.yunseong.website.manage.repository.RequestGeoStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestGeoStatisticsService {
    private static final int PAGE_SIZE = 10;
    private static final int BACKFILL_CHUNK = 500;

    private final RequestGeoStatisticsRepository requestGeoStatisticsRepository;
    private final GeoIpCountryResolver geoIpCountryResolver;

    @Transactional(readOnly = true)
    public Page<CountryStat> getCountriesForLastDays(int days, boolean includeBots, int page) {
        return requestGeoStatisticsRepository.findCountryCounts(
                LocalDateTime.now().minusDays(days), includeBots, PageRequest.of(page, PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public long getUnresolvedRequestsForLastDays(int days, boolean includeBots) {
        return requestGeoStatisticsRepository.countWithoutCountry(LocalDateTime.now().minusDays(days), includeBots);
    }

    public boolean isResolverAvailable() {
        return geoIpCountryResolver.isAvailable();
    }

    /**
     * Fills {@code country_code} for rows written before the column existed.
     * Walks distinct IPs in chunks and issues one UPDATE per IP, each in its own
     * transaction, so the table is never loaded into memory and an interrupted
     * run can simply be repeated.
     *
     * @return number of rows updated
     */
    public int backfillCountryCodes() {
        if (!geoIpCountryResolver.isAvailable()) {
            log.warn("Geo backfill skipped: no GeoIP database available");
            return 0;
        }
        int rowsUpdated = 0;
        int ipsSeen = 0;
        // ponytail: full distinct-IP scan per chunk (~4.2k IPs today). Add a
        // "pending IPs" projection if this ever stops being a one-off admin action.
        for (int chunk = 0; ; chunk++) {
            List<String> ips = requestGeoStatisticsRepository.findDistinctIps(PageRequest.of(chunk, BACKFILL_CHUNK));
            if (ips.isEmpty()) {
                break;
            }
            for (String ip : ips) {
                String countryCode = geoIpCountryResolver.resolveCountryCode(ip);
                if (countryCode != null) {
                    rowsUpdated += requestGeoStatisticsRepository.assignCountryCode(ip, countryCode);
                }
            }
            ipsSeen += ips.size();
            log.info("Geo backfill: {} IPs processed, {} rows updated", ipsSeen, rowsUpdated);
        }
        return rowsUpdated;
    }
}
