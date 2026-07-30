package dev.yunseong.website.manage.controller;

import dev.yunseong.website.manage.domain.TrafficDistribution;
import dev.yunseong.website.manage.service.TrafficDistributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/console")
@RequiredArgsConstructor
public class TrafficDistributionApiController {

    private final TrafficDistributionService trafficDistributionService;

    @GetMapping("/distribution")
    public ResponseEntity<TrafficDistribution> distribution(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "false") boolean includeBots) {
        return ResponseEntity.ok(trafficDistributionService.getDistributionForLastDays(days, includeBots));
    }
}
