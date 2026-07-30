package dev.yunseong.website.manage.controller;

import dev.yunseong.website.manage.domain.VisitorMetrics;
import dev.yunseong.website.manage.service.VisitorSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/console")
@RequiredArgsConstructor
public class VisitorApiController {

    private final VisitorSessionService visitorSessionService;

    @GetMapping("/visitors")
    public ResponseEntity<VisitorMetrics> visitors(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "true") boolean excludeBots) {
        return ResponseEntity.ok(visitorSessionService.getVisitorMetricsForLastDays(days, !excludeBots));
    }
}
