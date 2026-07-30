package dev.yunseong.website.manage.controller;

import dev.yunseong.website.manage.domain.CountryStat;
import dev.yunseong.website.manage.service.RequestGeoStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    /** Resolves {@code country_code} for rows written before the column existed. */
    @PostMapping("/backfill")
    public ResponseEntity<Map<String, Object>> backfill() {
        return ResponseEntity.ok(Map.of(
                "rowsUpdated", requestGeoStatisticsService.backfillCountryCodes(),
                "resolverAvailable", requestGeoStatisticsService.isResolverAvailable()
        ));
    }
}
