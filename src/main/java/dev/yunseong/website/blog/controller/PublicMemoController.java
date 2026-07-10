package dev.yunseong.website.blog.controller;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.service.CategoryService;
import dev.yunseong.website.blog.service.MemoService;
import dev.yunseong.website.global.util.AuthenticationUtil;
import dev.yunseong.website.global.util.MetadataUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@RequestMapping("/public/memos")
@RequiredArgsConstructor
public class PublicMemoController {

    private final MemoService memoService;
    private final CategoryService categoryService;

    @GetMapping
    public String blog(
            Model model,
            @RequestParam(required = false) String category,
            @SortDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication
    ) {
        boolean authenticated = AuthenticationUtil.isAuthenticated(authentication);
        Page<Memo> memos;
        if (category != null && !category.isEmpty()) {
            memos = authenticated ? memoService.getMemos(category, pageable) : memoService.getPublicMemos(category, pageable);
        } else {
            memos = authenticated ? memoService.getMemos(pageable) : memoService.getPublicMemos(pageable);
        }
        model.addAttribute("memos", memos);
        model.addAttribute("categoryTree", authenticated ? categoryService.getCategoryTree() : categoryService.getPublicCategoryTree());
        model.addAttribute("selectedCategory", category);
        
        return "blog";
    }

    @GetMapping("/{memoId}")
    public String showMemo(
            @PathVariable Long memoId,
            Model model,
            @SortDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        boolean authenticated = AuthenticationUtil.isAuthenticated(authentication);
        Memo memo = authenticated ? memoService.getMemo(memoId) : memoService.getPublicMemo(memoId);
        model.addAttribute("memo", memo);
        Page<Memo> memos = authenticated ? memoService.getMemos(memo.getName(), pageable) : memoService.getPublicMemos(memo.getName(), pageable);
        model.addAttribute("memos", memos);
        
        // Only set description for memo page, title/keywords handled in template
        model.addAttribute("pageDescription", MetadataUtil.extractContentPreview(memo.getContent()));
        
        return "memo/view";
    }
}
