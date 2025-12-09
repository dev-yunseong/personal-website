package com.yunseong.website.repository;

import com.yunseong.website.domain.GameProject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameProjectRepository extends JpaRepository<GameProject, Long> {
}
