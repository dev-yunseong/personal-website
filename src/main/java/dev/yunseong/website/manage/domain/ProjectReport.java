package dev.yunseong.website.manage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "project_reports")
@Getter
@NoArgsConstructor
public class ProjectReport {
    @Id
    @Column(length = 64)
    private String slug;
    @Column(nullable = false, columnDefinition = "text")
    private String payload;
    @Column(nullable = false)
    private Instant updatedAt;

    public ProjectReport(String slug, String payload, Instant updatedAt) {
        this.slug = slug;
        this.payload = payload;
        this.updatedAt = updatedAt;
    }
}
