package dev.yunseong.website.manage.controller;

import dev.yunseong.website.blog.service.MemoDraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/admin/console/drafts")
@RequiredArgsConstructor
public class ConsoleDraftController {
    private final MemoDraftService drafts;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("drafts", drafts.list(page));
        return "console/drafts";
    }

    @GetMapping("/{id}")
    public String review(@PathVariable long id, Model model) {
        model.addAttribute("review", drafts.review(id));
        return "console/draft_review";
    }

    @PutMapping("/{id}/approval")
    @ResponseBody
    public Map<String, String> approve(@PathVariable long id, @RequestBody Approval approval) {
        return Map.of("url", "/public/memos/" + drafts.approve(id, approval.revision()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> conflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "대상 경로가 변경되었습니다. 다시 검토하세요."));
    }

    public record Approval(String revision) {}
}
