package com.yunseong.website.controller;

import com.yunseong.website.domain.Memo;
import com.yunseong.website.service.MemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/public/memos")
@RequiredArgsConstructor
public class PublicMemoController {

    private final MemoService memoService;

    @GetMapping
    public String blog(Model model) {
        List<Memo> memos = memoService.getMemos();
        model.addAttribute("memos", memos);
        return "blog";
    }

    @GetMapping("/{memoId}")
    public String showMemo(
            @PathVariable Long memoId,
            Model model) {
        Memo memo = memoService.getMemo(memoId);
        model.addAttribute("memo", memo);
        return "memo/view";
    }
}
