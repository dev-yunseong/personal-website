package dev.yunseong.website.controller;

import dev.yunseong.website.domain.GameProject;
import dev.yunseong.website.service.GameProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/public/games")
@RequiredArgsConstructor
public class GameController {
    
    private final GameProjectService gameProjectService;
    
    @GetMapping("")
    public String listGames(Model model) {
        model.addAttribute("games", gameProjectService.getAllGameProjects());
        return "game/list";
    }
    
    @GetMapping("/{gameId}")
    public String showGame(@PathVariable Long gameId, Model model) {
        GameProject gameProject = gameProjectService.getGameProject(gameId);
        model.addAttribute("gameProject", gameProject);
        model.addAttribute("memo", gameProject.getMemo());
        
        return "game/play";
    }
}
