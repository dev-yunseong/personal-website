package dev.yunseong.website.manage.controller;

import dev.yunseong.website.manage.domain.RefererBreakdown;
import dev.yunseong.website.manage.domain.RefererStat;
import dev.yunseong.website.manage.service.RefererStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/console")
@RequiredArgsConstructor
public class RefererApiController {

    private final RefererStatisticsService refererStatisticsService;

    @GetMapping("/referer")
    public ResponseEntity<Map<String, Object>> referer(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "true") boolean excludeBots,
            @RequestParam(defaultValue = "0") int refererPage) {
        RefererBreakdown breakdown =
                refererStatisticsService.getRefererBreakdownForLastDays(days, excludeBots, refererPage);
        Page<RefererStat> domains = breakdown.domains();
        return ResponseEntity.ok(Map.of(
                "channels", breakdown.channels(),
                "content", domains.getContent(),
                "totalPages", domains.getTotalPages(),
                "totalElements", domains.getTotalElements(),
                "number", domains.getNumber(),
                "first", domains.isFirst(),
                "last", domains.isLast()
        ));
    }
}
