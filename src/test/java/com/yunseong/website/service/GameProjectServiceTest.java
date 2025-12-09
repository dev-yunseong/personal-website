package com.yunseong.website.service;

import com.yunseong.website.domain.GameProject;
import com.yunseong.website.domain.Memo;
import com.yunseong.website.repository.GameProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameProjectServiceTest {

    @Mock
    private GameProjectRepository gameProjectRepository;

    @InjectMocks
    private GameProjectService gameProjectService;

    private GameProject testGameProject;
    private Memo testMemo;

    @BeforeEach
    void setUp() {
        testMemo = new Memo("/project/word-online", "Word Online Game");
        testGameProject = new GameProject(1L, "https://word-online.yunseong.dev", testMemo);
    }

    @Test
    void getGameProject_WhenExists_ReturnsGameProject() {
        // Given
        when(gameProjectRepository.findById(1L))
            .thenReturn(Optional.of(testGameProject));

        // When
        GameProject result = gameProjectService.getGameProject(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("https://word-online.yunseong.dev", result.getGameUrl());
        assertNotNull(result.getMemo());
        verify(gameProjectRepository, times(1)).findById(1L);
    }

    @Test
    void getGameProject_WhenNotExists_ThrowsException() {
        // Given
        when(gameProjectRepository.findById(999L))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> gameProjectService.getGameProject(999L));
        verify(gameProjectRepository, times(1)).findById(999L);
    }

    @Test
    void getAllGameProjects_ReturnsAllProjects() {
        // Given
        List<GameProject> projects = Arrays.asList(testGameProject);
        when(gameProjectRepository.findAll()).thenReturn(projects);

        // When
        List<GameProject> result = gameProjectService.getAllGameProjects();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        verify(gameProjectRepository, times(1)).findAll();
    }

    @Test
    void createGameProject_CreatesAndReturnsGameProject() {
        // Given
        GameProject newGameProject = new GameProject("https://new-game.example.com", testMemo);
        when(gameProjectRepository.save(any(GameProject.class))).thenReturn(newGameProject);

        // When
        GameProject result = gameProjectService.createGameProject("https://new-game.example.com", testMemo);

        // Then
        assertNotNull(result);
        assertEquals("https://new-game.example.com", result.getGameUrl());
        verify(gameProjectRepository, times(1)).save(any(GameProject.class));
    }

    @Test
    void updateGameProject_UpdatesAndReturnsGameProject() {
        // Given
        when(gameProjectRepository.findById(1L)).thenReturn(Optional.of(testGameProject));
        when(gameProjectRepository.save(any(GameProject.class))).thenReturn(testGameProject);

        // When
        GameProject result = gameProjectService.updateGameProject(1L, "https://updated-url.example.com");

        // Then
        assertNotNull(result);
        assertEquals("https://updated-url.example.com", result.getGameUrl());
        verify(gameProjectRepository, times(1)).findById(1L);
        verify(gameProjectRepository, times(1)).save(testGameProject);
    }

    @Test
    void deleteGameProject_DeletesGameProject() {
        // When
        gameProjectService.deleteGameProject(1L);

        // Then
        verify(gameProjectRepository, times(1)).deleteById(1L);
    }
}
