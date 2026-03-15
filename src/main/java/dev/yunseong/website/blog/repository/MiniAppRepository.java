package dev.yunseong.website.blog.repository;

import dev.yunseong.website.blog.domain.MiniApp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MiniAppRepository extends JpaRepository<MiniApp, Long> {
}
