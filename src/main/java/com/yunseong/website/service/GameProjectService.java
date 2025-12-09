package com.yunseong.website.service;

import com.yunseong.website.domain.GameProject;
import com.yunseong.website.repository.GameProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameProjectService {
    
    private final GameProjectRepository gameProjectRepository;
    
    public GameProject getGameProject(String id) {
        return gameProjectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Game Project Not Found"));
    }
    
    public List<GameProject> getAllGameProjects() {
        return gameProjectRepository.findAll();
    }
}
