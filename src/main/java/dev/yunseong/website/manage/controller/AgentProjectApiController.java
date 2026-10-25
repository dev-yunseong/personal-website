package dev.yunseong.website.manage.controller;

import dev.yunseong.website.manage.service.ProjectReportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/agent/projects")
@RequiredArgsConstructor
public class AgentProjectApiController {
    private final ProjectReportService reports;

    @PutMapping(value = "/{slug}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> publish(@PathVariable String slug, HttpServletRequest request) throws IOException {
        byte[] body = request.getInputStream().readNBytes(ProjectReportService.MAX_BYTES + 1);
        reports.publish(slug, body);
        return ResponseEntity.noContent().location(URI.create("/admin/console/projects/" + slug)).build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalidReport(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }
}
