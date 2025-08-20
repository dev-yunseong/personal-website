package com.yunseong.website.controller;

import com.yunseong.website.domain.Memo;
import com.yunseong.website.service.MemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/memos")
@RequiredArgsConstructor
public class AdminMemoController {

    private final MemoService memoService;

    @GetMapping("/new")
    public String newMemoForm() {
        return "memo/new";
    }

    @PostMapping
    public String postMemo(
            @RequestParam String title,
            @RequestParam String content
    ) {
        memoService.saveMemo(title, content);
        return "redirect:/memos";
    }

    @GetMapping("/edit/{id}")
    public String editMemoForm(@PathVariable("id") long id, Model model) {
        Memo memo = memoService.getMemo(id);
        model.addAttribute("memo", memo);
        return "memo/edit";
    }

    @PostMapping("/edit/{id}")
    public String editMemo(@PathVariable("id") long id, @RequestParam String title, @RequestParam String content) {
        memoService.updateMemo(id, title, content);
        return "redirect:/memos";
    }
}
