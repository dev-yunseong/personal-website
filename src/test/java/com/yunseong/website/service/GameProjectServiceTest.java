package com.yunseong.website.service;

import com.yunseong.website.domain.GameProject;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameProjectServiceTest {

    @Mock
    private GameProjectRepository gameProjectRepository;

    @InjectMocks
    private GameProjectService gameProjectService;

    private GameProject testGameProject;

    @BeforeEach
    void setUp() {
        testGameProject = new GameProject(
            "word-online",
            "https://word-online.yunseong.dev",
            "/project/word-online"
        );
    }

    @Test
    void getGameProject_WhenExists_ReturnsGameProject() {
        // Given
        when(gameProjectRepository.findById("word-online"))
            .thenReturn(Optional.of(testGameProject));

        // When
        GameProject result = gameProjectService.getGameProject("word-online");

        // Then
        assertNotNull(result);
        assertEquals("word-online", result.getId());
        assertEquals("https://word-online.yunseong.dev", result.getGameUrl());
        assertEquals("/project/word-online", result.getBlogName());
        verify(gameProjectRepository, times(1)).findById("word-online");
    }

    @Test
    void getGameProject_WhenNotExists_ThrowsException() {
        // Given
        when(gameProjectRepository.findById("non-existent"))
            .thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> gameProjectService.getGameProject("non-existent"));
        verify(gameProjectRepository, times(1)).findById("non-existent");
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
        assertEquals("word-online", result.get(0).getId());
        verify(gameProjectRepository, times(1)).findAll();
    }
}
