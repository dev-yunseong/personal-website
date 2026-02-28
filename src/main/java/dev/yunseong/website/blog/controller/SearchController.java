package dev.yunseong.website.blog.controller;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.service.MemoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    public String search(@RequestParam String keyword, @PageableDefault(size = 10) Pageable pageable, Model model) {
        Page<Memo> searchResult = memoService.searchMemo(keyword, pageable);
        model.addAttribute("memos", searchResult);
        model.addAttribute("keyword", keyword);
        return "blog";
    }
}
