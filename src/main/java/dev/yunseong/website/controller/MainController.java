package dev.yunseong.website.controller;

import dev.yunseong.website.domain.Memo;
import dev.yunseong.website.service.MemoService;
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

    @GetMapping("")
    public String index(Model model) {
        try {
            Memo memo = memoService.getMemo("/README");
            model.addAttribute("memo", memo);
        } catch (IllegalArgumentException e) {
            model.addAttribute("memo", null);
        }
        return "index";
    }
}

