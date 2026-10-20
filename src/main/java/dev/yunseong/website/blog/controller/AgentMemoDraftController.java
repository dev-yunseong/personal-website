package dev.yunseong.website.blog.controller;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.service.MemoDraftService;
import dev.yunseong.website.blog.service.MemoNavigationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AgentMemoDraftController {
    private final MemoDraftService drafts;
    private final MemoNavigationService navigation;

    @GetMapping(value = "/api/agent/drafts/tree", produces = "text/plain;charset=UTF-8")
    public String tree(@RequestParam(defaultValue = "/") String path) {
        return navigation.tree(path, true);
    }

    @GetMapping("/api/agent/drafts/memos")
    public MemoNavigationService.NavigationPage list(@RequestParam(required = false) String category,
            @RequestParam(required = false) String updatedAfter,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "100") int limit) {
        return navigation.list(category, updatedAfter, page, limit, true);
    }

    @GetMapping(value = "/api/agent/drafts/content", produces = "text/markdown;charset=UTF-8")
    public String read(@RequestParam(required = false) Long memoId, @RequestParam(required = false) String target) {
        return navigation.readDraft(memoId, target);
    }

    @PostMapping(value = "/api/agent/drafts", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Map<String, Object> submit(@RequestParam String target, @RequestParam String content) {
        Memo draft = drafts.submit(target, content);
        return Map.of("id", draft.getId(), "name", draft.getName(),
                "reviewUrl", "/admin/console/drafts/" + draft.getId());
    }
}
