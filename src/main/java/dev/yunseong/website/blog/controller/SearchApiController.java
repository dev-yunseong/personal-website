package dev.yunseong.website.blog.controller;

import dev.yunseong.website.blog.domain.MemoSuggestion;
import dev.yunseong.website.blog.service.MemoService;
import dev.yunseong.website.global.util.AuthenticationUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/search")
public class SearchApiController {

    private final MemoService memoService;

    public SearchApiController(MemoService memoService) {
        this.memoService = memoService;
    }

    @GetMapping("/suggestions")
    public List<MemoSuggestion> getSuggestions(@RequestParam String keyword, Authentication authentication) {
        return (AuthenticationUtil.isAuthenticated(authentication)
                ? memoService.searchMemo(keyword, PageRequest.of(0, 5))
                : memoService.searchPublicMemo(keyword, PageRequest.of(0, 5))).getContent()
                .stream()
                .map(memo -> new MemoSuggestion(memo.getTitle()))
                .collect(Collectors.toList());
    }
}
