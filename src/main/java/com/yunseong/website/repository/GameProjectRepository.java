package com.yunseong.website.repository;

import com.yunseong.website.domain.GameProject;

import java.util.List;
import java.util.Optional;

public interface GameProjectRepository {
    Optional<GameProject> findById(String id);
    List<GameProject> findAll();
}
