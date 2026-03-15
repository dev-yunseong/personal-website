package dev.yunseong.website.blog.controller;

import dev.yunseong.website.blog.domain.MiniApp;
import dev.yunseong.website.blog.service.MiniAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequestMapping("/admin/miniapps")
@RequiredArgsConstructor
public class AdminMiniAppController {

    private final MiniAppService miniAppService;

    @GetMapping("")
    public String listMiniApps(Model model) {
        model.addAttribute("miniApps", miniAppService.getAllMiniApps());
        return "miniapp/list-admin";
    }

    @GetMapping("/new")
    public String newMiniAppForm() {
        return "miniapp/new";
    }

    @PostMapping("")
    public String createMiniApp(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String appUrl,
            @RequestParam(required = false) MultipartFile htmlFile
    ) throws IOException {
        if (htmlFile != null && !htmlFile.isEmpty()) {
            miniAppService.createMiniAppFromFile(title, description, htmlFile);
        } else if (appUrl != null && !appUrl.trim().isEmpty()) {
            miniAppService.createMiniApp(title, description, appUrl.trim());
        } else {
            return "redirect:/admin/miniapps/new?error=url_or_file_required";
        }
        return "redirect:/admin/miniapps";
    }

    @GetMapping("/edit/{id}")
    public String editMiniAppForm(@PathVariable Long id, Model model) {
        model.addAttribute("miniApp", miniAppService.getMiniApp(id));
        return "miniapp/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateMiniApp(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String appUrl,
            @RequestParam(required = false) MultipartFile htmlFile
    ) throws IOException {
        if (htmlFile != null && !htmlFile.isEmpty()) {
            miniAppService.updateMiniAppFromFile(id, title, description, htmlFile);
        } else if (appUrl != null && !appUrl.trim().isEmpty()) {
            miniAppService.updateMiniApp(id, title, description, appUrl.trim());
        } else {
            miniAppService.updateMiniAppTitleAndDescription(id, title, description);
        }
        return "redirect:/admin/miniapps";
    }

    @PostMapping("/delete/{id}")
    public String deleteMiniApp(@PathVariable Long id) {
        miniAppService.deleteMiniApp(id);
        return "redirect:/admin/miniapps";
    }
}
