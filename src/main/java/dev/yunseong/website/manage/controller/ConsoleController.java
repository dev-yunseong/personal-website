package dev.yunseong.website.manage.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/console")
@RequiredArgsConstructor
public class ConsoleController {

    @GetMapping
    public String console(Model model,
                          @RequestParam(defaultValue = "7") int days,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "0") int topPage,
                          @RequestParam(defaultValue = "") String statusFilter,
                          @RequestParam(defaultValue = "") String topStatusFilter,
                          @RequestParam(defaultValue = "count_desc") String topSort,
                          @RequestParam(defaultValue = "top") String tab,
                          @RequestParam(defaultValue = "uri") String topGroupBy,
                          @RequestParam(defaultValue = "") String timelineStatusFilter,
                          @RequestParam(defaultValue = "day") String timelineResolution) {
        model.addAttribute("days", days);
        model.addAttribute("page", page);
        model.addAttribute("topPage", topPage);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("topStatusFilter", topStatusFilter);
        model.addAttribute("topSort", topSort);
        model.addAttribute("tab", tab);
        model.addAttribute("topGroupBy", topGroupBy);
        model.addAttribute("timelineStatusFilter", timelineStatusFilter);
        model.addAttribute("timelineResolution", timelineResolution);
        return "console/dashboard";
    }
}
