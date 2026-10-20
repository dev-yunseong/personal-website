package dev.yunseong.website.blog.controller;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.service.CategoryService;
import dev.yunseong.website.blog.service.MemoService;
import dev.yunseong.website.blog.service.MemoNotFoundException;
import dev.yunseong.website.global.util.AuthenticationUtil;
import dev.yunseong.website.global.util.MetadataUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicMemoController {

    private final MemoService memoService;
    private final CategoryService categoryService;

    @GetMapping("/memos")
    public String blog(
            Model model,
            @RequestParam(required = false) String category,
            Pageable pageable,
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

    @GetMapping("/memos/{memoId}")
    public String showMemo(
            @PathVariable Long memoId,
            Model model,
            Pageable pageable,
            Authentication authentication) {
        boolean authenticated = AuthenticationUtil.isAuthenticated(authentication);
        Memo memo = authenticated ? memoService.getMemo(memoId) : memoService.getPublicMemo(memoId);
        return renderMemo(memo, model, pageable, authenticated);
    }

    @GetMapping("/blog/{*path}")
    public String showMemoByPath(
            @PathVariable String path,
            Model model,
            Pageable pageable,
            Authentication authentication) {
        boolean authenticated = AuthenticationUtil.isAuthenticated(authentication);
        if (!authenticated && path.startsWith(Memo.PRIVATE_PREFIX)) {
            throw new MemoNotFoundException();
        }
        if (path.isEmpty() || path.equals("/")) {
            return blog(model, null, pageable, authentication);
        }
        String category = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        return (authenticated ? memoService.findMemo(category) : memoService.findPublicMemo(category))
                .map(memo -> renderMemo(memo, model, pageable, authenticated))
                .orElseGet(() -> blog(model, category, pageable, authentication));
    }

    private String renderMemo(Memo memo, Model model, Pageable pageable, boolean authenticated) {
        model.addAttribute("memo", memo);
        Page<Memo> memos = authenticated ? memoService.getMemos(memo.getName(), pageable) : memoService.getPublicMemos(memo.getName(), pageable);
        model.addAttribute("memos", memos);
        
        // Only set description for memo page, title/keywords handled in template
        model.addAttribute("pageDescription", MetadataUtil.extractContentPreview(memo.getContent()));
        
        return "memo/view";
    }
}
