package dev.yunseong.website.manage.service;

import dev.yunseong.website.manage.domain.CityStat;
import dev.yunseong.website.manage.domain.CountryStat;
import dev.yunseong.website.manage.domain.GeoLocation;
import dev.yunseong.website.manage.domain.GeoRequestStat;
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
    private static final LocalDateTime ALL_TIME_START = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final int PAGE_SIZE = 10;
    private static final int REQUEST_PAGE_SIZE = 20;
    private static final int BACKFILL_CHUNK = 500;

    private final RequestGeoStatisticsRepository requestGeoStatisticsRepository;
    private final GeoIpLocationResolver geoIpLocationResolver;

    @Transactional(readOnly = true)
    public Page<CountryStat> getCountriesForLastDays(int days, boolean includeBots, int page) {
        return requestGeoStatisticsRepository.findCountryCounts(
                startDate(days), includeBots, PageRequest.of(page, PAGE_SIZE));
    }

    @Transactional(readOnly = true)
    public long getUnresolvedRequestsForLastDays(int days, boolean includeBots) {
        return requestGeoStatisticsRepository.countWithoutCountry(startDate(days), includeBots);
    }

    public boolean isResolverAvailable() {
        return geoIpLocationResolver.isAvailable();
    }

    @Transactional(readOnly = true)
    public List<CityStat> getCitiesForLastDays(int days, String countryCode, boolean includeBots) {
        return requestGeoStatisticsRepository.findCityCounts(
                startDate(days), countryCode, includeBots);
    }

    @Transactional(readOnly = true)
    public long getUnresolvedCitiesForLastDays(int days, String countryCode, boolean includeBots) {
        return requestGeoStatisticsRepository.countWithoutCity(
                startDate(days), countryCode, includeBots);
    }

    @Transactional(readOnly = true)
    public Page<GeoRequestStat> getRequestsForLastDays(
            int days, String countryCode, boolean includeBots, Long cityId, int page) {
        return requestGeoStatisticsRepository.findGeoRequests(
                startDate(days), countryCode, includeBots, cityId,
                PageRequest.of(page, REQUEST_PAGE_SIZE));
    }

    /**
     * Fills missing country and city fields for rows written before those columns existed.
     * Walks distinct IPs in chunks and issues one UPDATE per IP, each in its own
     * transaction, so the table is never loaded into memory and an interrupted
     * run can simply be repeated.
     *
     * @return number of rows updated
     */
    public int backfillLocations() {
        if (!geoIpLocationResolver.isAvailable()) {
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
                GeoLocation location = geoIpLocationResolver.resolve(ip);
                if (location != null) {
                    rowsUpdated += requestGeoStatisticsRepository.assignLocation(
                            ip, location.countryCode(), location.cityGeoNameId(), location.cityName(),
                            location.latitude(), location.longitude(), location.accuracyRadiusKm());
                }
            }
            ipsSeen += ips.size();
            log.info("Geo backfill: {} IPs processed, {} rows updated", ipsSeen, rowsUpdated);
        }
        return rowsUpdated;
    }

    private static LocalDateTime startDate(int days) {
        return days == 0 ? ALL_TIME_START : LocalDateTime.now().minusDays(days);
    }
}
