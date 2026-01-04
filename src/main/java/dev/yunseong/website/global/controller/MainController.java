package dev.yunseong.website.global.controller;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.service.MemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/")
@Controller
@RequiredArgsConstructor
public class MainController {

    private final MemoService memoService;

    @GetMapping
    public String index(Model model) {
        try {
            Memo memo = memoService.getMemo("/README");
            model.addAttribute("memo", memo);
        } catch (IllegalArgumentException e) {
            model.addAttribute("memo", null);
        }
        
        // Add metadata for the main page
        model.addAttribute("pageTitle", "Yunseong - Personal Website");
        model.addAttribute("pageDescription", "Yunseong의 개인 웹사이트입니다. 개발, 기술 블로그, 그리고 다양한 프로젝트를 공유합니다.");
        model.addAttribute("pageKeywords", "개발, 블로그, Yunseong, 프로그래밍, 기술, 웹개발");
        model.addAttribute("pageUrl", "https://yunseong.dev");
        
        return "index";
    }
}

