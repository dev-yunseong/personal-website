package dev.yunseong.website.blog.controller;

import dev.yunseong.website.blog.domain.GameProject;
import dev.yunseong.website.blog.service.GameProjectService;
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
    
    private static final String BASE_URL = "https://yunseong.dev";
    
    private final GameProjectService gameProjectService;
    
    @GetMapping("")
    public String listGames(Model model) {
        model.addAttribute("games", gameProjectService.getAllGameProjects());
        
        // Add metadata for the games list page
        model.addAttribute("pageTitle", "Games | Yunseong");
        model.addAttribute("pageDescription", "Yunseong의 게임 프로젝트 모음입니다. 다양한 게임을 플레이해보세요.");
        model.addAttribute("pageKeywords", "게임, 게임 프로젝트, 웹 게임, 개발");
        model.addAttribute("pageUrl", BASE_URL + "/public/games");
        
        return "game/list";
    }
    
    @GetMapping("/{gameId}")
    public String showGame(@PathVariable Long gameId, Model model) {
        GameProject gameProject = gameProjectService.getGameProject(gameId);
        model.addAttribute("gameProject", gameProject);
        model.addAttribute("memo", gameProject.getMemo());
        
        // Add metadata for the individual game page
        String gameTitle = gameProject.getTitle();
        model.addAttribute("pageTitle", gameTitle + " | Yunseong");
        model.addAttribute("pageDescription", gameTitle + " - Yunseong의 게임 프로젝트입니다.");
        model.addAttribute("pageKeywords", gameTitle + ", 게임, 웹 게임, 프로젝트");
        model.addAttribute("pageUrl", BASE_URL + "/public/games/" + gameId);
        
        return "game/play";
    }
}
