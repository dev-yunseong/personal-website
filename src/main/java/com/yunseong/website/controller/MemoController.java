package com.yunseong.website.controller;

import com.yunseong.website.domain.Memo;
import com.yunseong.website.service.MemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/memos")
@RequiredArgsConstructor
public class MemoController {

    private final MemoService memoService;

    @GetMapping
    public String blog(Model model) {
        List<Memo> memos = memoService.getMemos();
        model.addAttribute("memos", memos);
        return "blog";
    }

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

    @GetMapping("/{memoId}")
    public String showMemo(
            @PathVariable Long memoId,
            Model model) {
        Memo memo = memoService.getMemo(memoId);
        model.addAttribute("memo", memo);
        return "memo/view";
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
