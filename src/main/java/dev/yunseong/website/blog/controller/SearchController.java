package dev.yunseong.website.blog.controller;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.service.MemoService;
import dev.yunseong.website.global.util.AuthenticationUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SearchController {

    private final MemoService memoService;

    public SearchController(MemoService memoService) {
        this.memoService = memoService;
    }

    @GetMapping("/public/search")
    public String search(@RequestParam String keyword, @PageableDefault(size = 10) Pageable pageable, Model model,
            Authentication authentication) {
        Page<Memo> searchResult = AuthenticationUtil.isAuthenticated(authentication)
                ? memoService.searchMemo(keyword, pageable)
                : memoService.searchPublicMemo(keyword, pageable);
        model.addAttribute("memos", searchResult);
        model.addAttribute("keyword", keyword);
        return "blog";
    }
}
