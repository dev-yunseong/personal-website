package dev.yunseong.website.blog.controller;

import dev.yunseong.website.blog.domain.MiniApp;
import dev.yunseong.website.blog.service.MiniAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/public/apps")
@RequiredArgsConstructor
public class PublicMiniAppController {

    private final MiniAppService miniAppService;

    @GetMapping("")
    public String listMiniApps(Model model) {
        model.addAttribute("miniApps", miniAppService.getAllMiniApps());
        return "miniapp/list";
    }

    @GetMapping("/{id}")
    public String showMiniApp(@PathVariable Long id, Model model) {
        MiniApp miniApp = miniAppService.getMiniApp(id);
        model.addAttribute("miniApp", miniApp);
        return "miniapp/play";
    }
}
