package dev.yunseong.website.controller;

import dev.yunseong.website.domain.GameProject;
import dev.yunseong.website.domain.Memo;
import dev.yunseong.website.service.GameProjectService;
import dev.yunseong.website.service.MemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/memos")
@RequiredArgsConstructor
public class AdminMemoController {

    private final MemoService memoService;
    private final GameProjectService gameProjectService;

    @GetMapping("/new")
    public String newMemoForm(@RequestParam String category, Model model) {
        model.addAttribute("category", category);
        return "memo/new";
    }

    @PostMapping
    public String postMemo(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) Boolean isGameProject,
            @RequestParam(required = false) String gameUrl
    ) {
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
            @RequestParam(required = false) String gameUrl
    ) {
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
}
