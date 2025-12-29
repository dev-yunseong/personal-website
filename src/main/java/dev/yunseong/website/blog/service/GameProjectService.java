package dev.yunseong.website.blog.service;

import dev.yunseong.website.blog.domain.GameProject;
import dev.yunseong.website.blog.domain.Memo;
import dev.yunseong.website.blog.repository.GameProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GameProjectService {
    
    private final GameProjectRepository gameProjectRepository;
    
    @Transactional(readOnly = true)
    public GameProject getGameProject(Long id) {
        return gameProjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Game Project Not Found"));
    }
    
    @Transactional(readOnly = true)
    public List<GameProject> getAllGameProjects() {
        return gameProjectRepository.findAll();
    }
    
    public GameProject createGameProject(String gameUrl, Memo memo) {
        GameProject gameProject = new GameProject(gameUrl, memo);
        return gameProjectRepository.save(gameProject);
    }
    
    public GameProject updateGameProject(Long id, String gameUrl) {
        GameProject gameProject = getGameProject(id);
        gameProject.setGameUrl(gameUrl);
        return gameProjectRepository.save(gameProject);
    }
    
    public void deleteGameProject(Long id) {
        gameProjectRepository.deleteById(id);
    }
}
