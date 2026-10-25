package dev.yunseong.website.manage.repository;

import dev.yunseong.website.manage.domain.ProjectReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectReportRepository extends JpaRepository<ProjectReport, String> {
    List<ProjectReport> findAllByOrderByUpdatedAtDescSlugAsc();
}
