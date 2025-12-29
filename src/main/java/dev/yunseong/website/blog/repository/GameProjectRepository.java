package dev.yunseong.website.blog.repository;

import dev.yunseong.website.blog.domain.GameProject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameProjectRepository extends JpaRepository<GameProject, Long> {
}
