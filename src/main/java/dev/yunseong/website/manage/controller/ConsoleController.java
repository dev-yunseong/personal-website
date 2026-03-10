package dev.yunseong.website.manage.controller;

import dev.yunseong.website.manage.domain.RequestStatistics;
import dev.yunseong.website.manage.domain.UriStat;
import dev.yunseong.website.manage.service.RequestStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/console")
@RequiredArgsConstructor
public class ConsoleController {

    private static final int PAGE_SIZE = 10;

    private final RequestStatisticsService requestStatisticsService;

    @GetMapping
    public String console(Model model,
                          @RequestParam(defaultValue = "7") int days,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "0") int topPage) {
        Pageable historyPageable = PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
        Pageable topPageable = PageRequest.of(topPage, PAGE_SIZE);

        Page<RequestStatistics> statistics = requestStatisticsService.getStatisticsForLastDays(days, historyPageable);
        Page<UriStat> topUris = requestStatisticsService.getTopUrisPageForLastDays(days, topPageable);
        long totalRequests = requestStatisticsService.getTotalRequestsForLastDays(days);

        model.addAttribute("statistics", statistics);
        model.addAttribute("topUris", topUris);
        model.addAttribute("totalRequests", totalRequests);
        model.addAttribute("days", days);
        model.addAttribute("page", page);
        model.addAttribute("topPage", topPage);

        return "console/dashboard";
    }
}
