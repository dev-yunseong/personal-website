package dev.yunseong.website.blog.controller;

import dev.yunseong.website.blog.domain.GameProject;
import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.service.GameProjectService;
import dev.yunseong.website.blog.service.MemoService;
import dev.yunseong.website.global.service.ProfileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/admin/memos")
@RequiredArgsConstructor
public class AdminMemoController {

    private final MemoService memoService;
    private final GameProjectService gameProjectService;
    private final ProfileService profileService;

    @GetMapping("/new")
    public String newMemoForm(@RequestParam String category, Model model) {
        model.addAttribute("category", category);
        model.addAttribute("isProfileMemo", isProfileMemo(category));
        return "memo/new";
    }

    @PostMapping
    public String postMemo(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) Boolean isGameProject,
            @RequestParam(required = false) String gameUrl,
            Model model,
            HttpServletResponse response
    ) {
        if (!validateProfileBeforeSave(title, content, model)) {
            model.addAttribute("category", title);
            model.addAttribute("content", content);
            model.addAttribute("isProfileMemo", true);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return "memo/new";
        }

        long memoId = memoService.saveMemo(title, content);
        
        if (Boolean.TRUE.equals(isGameProject) && gameUrl != null && !gameUrl.trim().isEmpty()) {
            Memo memo = memoService.getMemo(memoId);
            gameProjectService.createGameProject(gameUrl.trim(), memo);
        }
        
        return String.format("redirect:/public/memos/%d", memoId);
    }

    @GetMapping("/edit/{id}")
    public String editMemoForm(@PathVariable("id") long id, Model model) {
        Memo memo = memoService.getMemo(id);
        model.addAttribute("memo", memo);
        model.addAttribute("isProfileMemo", isProfileMemo(memo.getName()));
        
        // Check if this memo has an associated game project (only first one is used per design)
        if (!memo.getGameProjects().isEmpty()) {
            GameProject gameProject = memo.getGameProjects().get(0);
            model.addAttribute("gameProject", gameProject);
        }
        
        return "memo/edit";
    }

    @PostMapping("/edit/{id}")
    public String editMemo(
            @PathVariable("id") long memoId, 
            @RequestParam String title, 
            @RequestParam String content,
            @RequestParam(required = false) Boolean isGameProject,
            @RequestParam(required = false) String gameUrl,
            Model model,
            HttpServletResponse response
    ) {
        if (!validateProfileBeforeSave(title, content, model)) {
            Memo memo = memoService.getMemo(memoId);
            memo.setName(title);
            memo.setContent(content);
            model.addAttribute("memo", memo);
            model.addAttribute("isProfileMemo", true);
            addGameProject(model, memo);
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return "memo/edit";
        }

        memoService.updateMemo(memoId, title, content);
        Memo memo = memoService.getMemo(memoId);
        
        if (Boolean.TRUE.equals(isGameProject) && gameUrl != null && !gameUrl.trim().isEmpty()) {
            // Update existing game project or create new one (only first one per memo by design)
            GameProject existingGameProject = getFirstGameProject(memo);
            if (existingGameProject != null) {
                gameProjectService.updateGameProject(existingGameProject.getId(), gameUrl.trim());
            } else {
                gameProjectService.createGameProject(gameUrl.trim(), memo);
            }
        } else if (Boolean.FALSE.equals(isGameProject)) {
            // Remove game project if checkbox is unchecked
            GameProject gameProject = getFirstGameProject(memo);
            if (gameProject != null) {
                gameProjectService.deleteGameProject(gameProject.getId());
            }
        }
        
        return String.format("redirect:/public/memos/%d", memoId);
    }

    private GameProject getFirstGameProject(Memo memo) {
        return memo.getGameProjects().isEmpty() ? null : memo.getGameProjects().get(0);
    }

    private void addGameProject(Model model, Memo memo) {
        GameProject gameProject = getFirstGameProject(memo);
        if (gameProject != null) {
            model.addAttribute("gameProject", gameProject);
        }
    }

    private boolean validateProfileBeforeSave(String title, String content, Model model) {
        if (!isProfileMemo(title)) {
            return true;
        }
        try {
            profileService.validate(content);
            return true;
        } catch (IllegalArgumentException e) {
            model.addAttribute("validationError", e.getMessage());
            return false;
        }
    }

    private boolean isProfileMemo(String title) {
        return "/profile".equals(title) || "profile".equals(title);
    }

    @PostMapping(value = "/profile/validate", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateProfile(
            @RequestParam(defaultValue = "") String content
    ) {
        try {
            profileService.validate(content);
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "message", "Valid profile YAML. Safe to save."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping(value = "/preview", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public ResponseEntity<String> previewMemo(@RequestParam(defaultValue = "") String content) {
        Memo memo = new Memo();
        memo.setContent(content);
        return ResponseEntity.ok(memo.getHtml());
    }
}
