package com.yunseong.website.controller;

import com.yunseong.website.domain.Memo;
import com.yunseong.website.service.CategoryService;
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
    private final CategoryService categoryService;

    @GetMapping
    public String blog(Model model, @RequestParam(required = false) String category) {
        List<Memo> memos;
        if (category != null && !category.isEmpty()) {
            memos = memoService.getMemos(category);
        } else {
            memos = memoService.getMemos();
        }
        List<String> categories = categoryService.getCategories();
        model.addAttribute("memos", memos);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", category);
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
