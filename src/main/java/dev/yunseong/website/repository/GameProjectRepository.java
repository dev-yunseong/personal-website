package dev.yunseong.website.repository;

import dev.yunseong.website.domain.GameProject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameProjectRepository extends JpaRepository<GameProject, Long> {
}
