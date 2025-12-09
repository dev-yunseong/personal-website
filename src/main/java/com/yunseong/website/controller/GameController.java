package com.yunseong.website.controller;

import com.yunseong.website.domain.GameProject;
import com.yunseong.website.domain.Memo;
import com.yunseong.website.service.GameProjectService;
import com.yunseong.website.service.MemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/games")
@RequiredArgsConstructor
public class GameController {
    
    private final GameProjectService gameProjectService;
    private final MemoService memoService;
    
    @GetMapping("/{gameId}")
    public String showGame(@PathVariable String gameId, Model model) {
        GameProject gameProject = gameProjectService.getGameProject(gameId);
        model.addAttribute("gameProject", gameProject);
        
        try {
            Memo memo = memoService.getMemo(gameProject.getBlogName());
            model.addAttribute("memo", memo);
        } catch (IllegalArgumentException e) {
            model.addAttribute("memo", null);
        }
        
        return "games";
    }
}
