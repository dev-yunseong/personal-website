package dev.yunseong.website.manage.controller;

import dev.yunseong.website.manage.domain.CityStat;
import dev.yunseong.website.manage.domain.CountryStat;
import dev.yunseong.website.manage.domain.GeoRequestStat;
import dev.yunseong.website.manage.service.RequestGeoStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/console/geo")
@RequiredArgsConstructor
public class ConsoleGeoApiController {

    private final RequestGeoStatisticsService requestGeoStatisticsService;

    @GetMapping("/countries")
    public ResponseEntity<Map<String, Object>> countries(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "0") int geoPage,
            @RequestParam(defaultValue = "false") boolean includeBots) {
        validateDays(days);
        validatePage(geoPage);
        Page<CountryStat> page = requestGeoStatisticsService.getCountriesForLastDays(days, includeBots, geoPage);
        return ResponseEntity.ok(Map.of(
                "content", page.getContent(),
                "totalPages", page.getTotalPages(),
                "totalElements", page.getTotalElements(),
                "number", page.getNumber(),
                "first", page.isFirst(),
                "last", page.isLast(),
                "unresolvedRequests", requestGeoStatisticsService.getUnresolvedRequestsForLastDays(days, includeBots),
                "resolverAvailable", requestGeoStatisticsService.isResolverAvailable()
        ));
    }

    @GetMapping("/countries/{countryCode}/cities")
    public ResponseEntity<Map<String, Object>> cities(
            @PathVariable String countryCode,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "false") boolean includeBots) {
        validateDays(days);
        String normalizedCountryCode = normalizeCountryCode(countryCode);
        List<CityStat> cities = requestGeoStatisticsService.getCitiesForLastDays(
                days, normalizedCountryCode, includeBots);
        return ResponseEntity.ok(Map.of(
                "content", cities,
                "unresolvedRequests", requestGeoStatisticsService.getUnresolvedCitiesForLastDays(
                        days, normalizedCountryCode, includeBots)
        ));
    }

    @GetMapping("/countries/{countryCode}/requests")
    public ResponseEntity<Page<GeoRequestStat>> requests(
            @PathVariable String countryCode,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "false") boolean includeBots,
            @RequestParam(required = false) Long cityId,
            @RequestParam(defaultValue = "0") int page) {
        validateDays(days);
        validatePage(page);
        if (cityId != null && cityId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cityId must be positive");
        }
        return ResponseEntity.ok(requestGeoStatisticsService.getRequestsForLastDays(
                days, normalizeCountryCode(countryCode), includeBots, cityId, page));
    }

    /** Resolves missing country and city fields for existing request rows. */
    @PostMapping("/backfill")
    public ResponseEntity<Map<String, Object>> backfill() {
        return ResponseEntity.ok(Map.of(
                "rowsUpdated", requestGeoStatisticsService.backfillLocations(),
                "resolverAvailable", requestGeoStatisticsService.isResolverAvailable()
        ));
    }

    private static void validateDays(int days) {
        if (days < 0 || days > 90) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "days must be 0 (all time) or between 1 and 90");
        }
    }

    private static void validatePage(int page) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must not be negative");
        }
    }

    private static String normalizeCountryCode(String countryCode) {
        String normalized = countryCode == null ? "" : countryCode.toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{2}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "countryCode must be ISO alpha-2");
        }
        return normalized;
    }
}
