package com.yunseong.website.controller;

import com.yunseong.website.domain.RequestStatistics;
import com.yunseong.website.service.RequestStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/console")
@RequiredArgsConstructor
public class ConsoleController {
    private final RequestStatisticsService requestStatisticsService;

    @GetMapping
    public String console(Model model) {
        // Get statistics for the last 7 days
        List<RequestStatistics> statistics = requestStatisticsService.getStatisticsForLastDays(7);
        Map<String, Long> topUris = requestStatisticsService.getTopUrisForLastDays(7);
        long totalRequests = requestStatisticsService.getTotalRequestsForLastDays(7);

        model.addAttribute("statistics", statistics);
        model.addAttribute("topUris", topUris);
        model.addAttribute("totalRequests", totalRequests);

        return "console/dashboard";
    }
}
