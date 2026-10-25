package dev.yunseong.website.manage.controller;

import dev.yunseong.website.manage.service.ProjectReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequiredArgsConstructor
public class ProjectConsoleController {
    private final ProjectReportService reports;

    @GetMapping("/admin/console")
    public String overview(Model model, @RequestParam(required = false) String tab) {
        // 기존 통계 tab bookmark도 같은 화면을 표시한다.
        if (tab != null) {
            return "forward:/admin/console/traffic";
        }
        var projects = reports.findAll();
        model.addAttribute("pageTitle", "프로젝트 개요 · Console");
        model.addAttribute("projects", projects);
        model.addAttribute("staleCount", projects.stream().filter(project -> project.stale()).count());
        model.addAttribute("blockedCount", projects.stream()
                .filter(project -> project.content().status() == dev.yunseong.website.manage.domain.ProjectReportContent.Status.BLOCKED).count());
        model.addAttribute("adminSection", "overview");
        return "console/overview";
    }

    @GetMapping("/admin/console/projects/{slug}")
    public String project(@PathVariable String slug, Model model) {
        var project = reports.find(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("pageTitle", project.content().name() + " · Console");
        model.addAttribute("project", project);
        model.addAttribute("adminSection", "overview");
        return "console/project";
    }
}
