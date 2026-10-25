package dev.yunseong.website.manage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.yunseong.website.manage.domain.ProjectOverview;
import dev.yunseong.website.manage.domain.ProjectReport;
import dev.yunseong.website.manage.domain.ProjectReportContent;
import dev.yunseong.website.manage.repository.ProjectReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectReportService {
    public static final int MAX_BYTES = 65_536;
    private final ProjectReportRepository reports;
    private final ObjectMapper objectMapper;

    @Transactional
    public void publish(String slug, byte[] body) {
        if (!slug.matches("[a-z0-9][a-z0-9-]{0,63}")) {
            throw new IllegalArgumentException("project는 소문자, 숫자, 하이픈으로 1~64자여야 합니다.");
        }
        if (body.length > MAX_BYTES) {
            throw new IllegalArgumentException("보고서는 64 KiB 이하여야 합니다.");
        }
        ProjectReportContent content;
        try {
            content = objectMapper.readValue(body, ProjectReportContent.class);
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("보고서 JSON 형식을 확인해 주세요.");
        }
        validate(content);
        // 검증한 필드만 저장해 client가 보낸 알 수 없는 필드는 제거한다.
        try {
            reports.save(new ProjectReport(slug, objectMapper.writeValueAsString(content), Instant.now()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize project report", exception);
        }
    }

    public List<ProjectOverview> findAll() {
        Instant now = Instant.now();
        return reports.findAllByOrderByUpdatedAtDescSlugAsc().stream()
                .map(report -> overview(report, now)).toList();
    }

    public Optional<ProjectOverview> find(String slug) {
        return reports.findById(slug).map(report -> overview(report, Instant.now()));
    }

    private ProjectOverview overview(ProjectReport report, Instant now) {
        try {
            ProjectReportContent content = objectMapper.readValue(report.getPayload(), ProjectReportContent.class);
            boolean stale = report.getUpdatedAt().plus(content.staleAfterMinutes(), ChronoUnit.MINUTES).isBefore(now);
            return new ProjectOverview(report.getSlug(), content, report.getUpdatedAt(), stale);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot read project report: " + report.getSlug(), exception);
        }
    }

    private void validate(ProjectReportContent report) {
        require(report != null, "보고서가 필요합니다.");
        text(report.name(), 100, "name");
        text(report.agent(), 100, "agent");
        text(report.summary(), 2000, "summary");
        require(report.status() != null, "status가 필요합니다.");
        require(report.staleAfterMinutes() >= 5 && report.staleAfterMinutes() <= 10_080,
                "staleAfterMinutes는 5~10080이어야 합니다.");
        require(report.widgets() != null && report.widgets().size() <= 12, "widgets는 최대 12개입니다.");
        for (var widget : report.widgets()) {
            require(widget != null && widget.type() != null, "widget type이 필요합니다.");
            text(widget.title(), 100, "widget title");
            switch (widget.type()) {
                case METRIC -> text(widget.value(), 100, "metric value");
                case TABLE -> {
                    require(widget.columns() != null && !widget.columns().isEmpty()
                            && widget.columns().size() <= 8, "table columns는 1~8개입니다.");
                    widget.columns().forEach(column -> text(column, 100, "column"));
                    require(widget.rows() != null && widget.rows().size() <= 100, "table rows는 최대 100개입니다.");
                    for (var row : widget.rows()) {
                        require(row != null && row.size() == widget.columns().size(), "표의 열 개수가 맞지 않습니다.");
                        row.forEach(cell -> text(cell, 500, "cell"));
                    }
                }
                case LINE -> {
                    require(widget.points() != null && !widget.points().isEmpty()
                            && widget.points().size() <= 100, "line points는 1~100개입니다.");
                    for (var point : widget.points()) {
                        require(point != null, "point가 필요합니다.");
                        text(point.label(), 100, "point label");
                        require(point.value() != null && Double.isFinite(point.value()) && Math.abs(point.value()) <= 1e12,
                                "point value는 유한한 수이고 절댓값이 1조 이하여야 합니다.");
                    }
                }
            }
        }
    }

    private void text(String value, int max, String field) {
        require(value != null && !value.isBlank() && value.length() <= max,
                field + "는 비어 있지 않은 " + max + "자 이하 문자열이어야 합니다.");
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
