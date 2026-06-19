package dev.yunseong.website.global.controller;

import dev.yunseong.website.global.domain.ProfileLoadResult;
import dev.yunseong.website.global.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/")
@Controller
@RequiredArgsConstructor
public class MainController {

    private final ProfileService profileService;

    @Value("${app.profile-image-url:}")
    private String profileImageUrl;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("profileImageUrl", profileImageUrl);
        ProfileLoadResult profileResult = profileService.getProfile();
        model.addAttribute("profile", profileResult.profile());
        model.addAttribute("profileStatus", profileResult.status());
        return "index";
    }
}
