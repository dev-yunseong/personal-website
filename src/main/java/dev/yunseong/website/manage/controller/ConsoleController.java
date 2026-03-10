package dev.yunseong.website.manage.controller;

import dev.yunseong.website.manage.domain.RequestStatistics;
import dev.yunseong.website.manage.service.RequestStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping("/admin/console")
@RequiredArgsConstructor
public class ConsoleController {
    private final RequestStatisticsService requestStatisticsService;

    @GetMapping
    public String console(Model model,
                          @RequestParam(defaultValue = "7") int days,
                          @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<RequestStatistics> statistics = requestStatisticsService.getStatisticsForLastDays(days, pageable);
        Map<String, Long> topUris = requestStatisticsService.getTopUrisForLastDays(days);
        long totalRequests = requestStatisticsService.getTotalRequestsForLastDays(days);

        model.addAttribute("statistics", statistics);
        model.addAttribute("topUris", topUris);
        model.addAttribute("totalRequests", totalRequests);
        model.addAttribute("days", days);

        return "console/dashboard";
    }
}
