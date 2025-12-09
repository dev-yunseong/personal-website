package com.yunseong.website.repository;

import com.yunseong.website.domain.GameProject;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class InMemoryGameProjectRepository implements GameProjectRepository {
    
    private final Map<String, GameProject> gameProjects = new HashMap<>();
    
    public InMemoryGameProjectRepository() {
        // Initialize with test data
        GameProject wordOnline = new GameProject(
            "word-online",
            "https://word-online.yunseong.dev",
            "/project/word-online"
        );
        gameProjects.put(wordOnline.getId(), wordOnline);
        
        // Add more test games if needed
        GameProject dudoji = new GameProject(
            "dudoji",
            "https://dudoji.yunseong.shop:36073",
            "/project/dudoji"
        );
        gameProjects.put(dudoji.getId(), dudoji);
    }
    
    @Override
    public Optional<GameProject> findById(String id) {
        return Optional.ofNullable(gameProjects.get(id));
    }
    
    @Override
    public List<GameProject> findAll() {
        return new ArrayList<>(gameProjects.values());
    }
}
