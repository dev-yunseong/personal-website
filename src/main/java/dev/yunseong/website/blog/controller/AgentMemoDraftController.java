package dev.yunseong.website.blog.controller;

import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.service.MemoDraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AgentMemoDraftController {
    private final MemoDraftService drafts;

    @PostMapping(value = "/api/agent/drafts", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Map<String, Object> submit(@RequestParam String target, @RequestParam String content) {
        Memo draft = drafts.submit(target, content);
        return Map.of("id", draft.getId(), "name", draft.getName(),
                "reviewUrl", "/admin/console/drafts/" + draft.getId());
    }
}
