package dev.yunseong.website.blog.controller;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.service.CategoryService;
import dev.yunseong.website.blog.service.MemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/public/memos")
@RequiredArgsConstructor
public class PublicMemoController {

    private static final int CONTENT_PREVIEW_LENGTH = 150;
    private static final String MARKDOWN_CHARS_REGEX = "[#*_`\\[\\]()]";
    private static final String BASE_URL = "https://yunseong.dev";

    private final MemoService memoService;
    private final CategoryService categoryService;

    @GetMapping
    public String blog(
            Model model,
            @RequestParam(required = false) String category,
            @SortDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<Memo> memos;
        if (category != null && !category.isEmpty()) {
            memos = memoService.getMemos(category, pageable);
        } else {
            memos = memoService.getMemos(pageable);
        }
        model.addAttribute("memos", memos);
        model.addAttribute("categoryTree", categoryService.getCategoryTree());
        model.addAttribute("selectedCategory", category);
        
        // Add metadata for the blog page
        String pageTitle = category != null && !category.isEmpty() 
            ? "Blog - " + category + " | Yunseong" 
            : "Blog | Yunseong";
        String pageDescription = category != null && !category.isEmpty()
            ? "Yunseong의 블로그 - " + category + " 카테고리의 게시글을 확인하세요."
            : "Yunseong의 기술 블로그입니다. 개발 관련 글과 프로젝트를 공유합니다.";
        
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("pageDescription", pageDescription);
        model.addAttribute("pageKeywords", "블로그, 개발, 기술, 프로그래밍" + (category != null ? ", " + category : ""));
        model.addAttribute("pageUrl", BASE_URL + "/public/memos" + (category != null ? "?category=" + category : ""));
        
        return "blog";
    }

    @GetMapping("/{memoId}")
    public String showMemo(
            @PathVariable Long memoId,
            Model model,
            @SortDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Memo memo = memoService.getMemo(memoId);
        model.addAttribute("memo", memo);
        Page<Memo> memos = memoService.getMemos(memo.getName(),pageable);
        model.addAttribute("memos", memos);
        
        // Add metadata for individual memo page
        String memoTitle = memo.getTitle();
        String pageTitle = memoTitle + " | Yunseong";
        
        // Extract description from content (first 150 characters of plain text)
        String contentPreview = memo.getContent() != null && memo.getContent().length() > CONTENT_PREVIEW_LENGTH
            ? memo.getContent().substring(0, CONTENT_PREVIEW_LENGTH).replaceAll(MARKDOWN_CHARS_REGEX, "") + "..." 
            : "Yunseong의 블로그 포스트입니다.";
        
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("pageDescription", contentPreview);
        model.addAttribute("pageKeywords", memoTitle + ", " + memo.getPath() + ", 블로그, 개발");
        model.addAttribute("pageUrl", BASE_URL + "/public/memos/" + memoId);
        
        return "memo/view";
    }
}
