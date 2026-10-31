package dev.yunseong.website.manage.controller;

import dev.yunseong.website.manage.service.OperationActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/console/projects/{slug}/actions")
@RequiredArgsConstructor
public class OperationActionController {
    private final OperationActionService actions;

    @PostMapping("/{key}")
    public String enqueue(@PathVariable String slug, @PathVariable String key,
                          @RequestParam(defaultValue = "false") boolean confirmed,
                          Principal principal, RedirectAttributes redirect) {
        try { actions.enqueue(slug, key, principal.getName(), confirmed); redirect.addFlashAttribute("actionMessage", "action을 실행 대기열에 추가했습니다."); }
        catch (IllegalArgumentException | IllegalStateException exception) { redirect.addFlashAttribute("actionError", exception.getMessage()); }
        return "redirect:/admin/console/projects/" + slug;
    }
}
