package dev.yunseong.website.manage.controller;

import dev.yunseong.website.manage.domain.OperationAction;
import dev.yunseong.website.manage.service.OperationActionService;
import dev.yunseong.website.manage.service.ProjectReportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/runner/projects/{slug}")
@RequiredArgsConstructor
public class RunnerOperationsApiController {
    private final ProjectReportService reports;
    private final OperationActionService actions;

    @PutMapping(value = "/report", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> report(@PathVariable String slug, HttpServletRequest request) throws IOException {
        reports.publish(slug, request.getInputStream().readNBytes(ProjectReportService.MAX_BYTES + 1));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/actions/claim")
    public ResponseEntity<OperationActionService.Claim> claim(@PathVariable String slug) {
        return actions.claim(slug).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping(value = "/actions/{id}/result", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> result(@PathVariable String slug, @PathVariable String id, @RequestBody Result result) {
        actions.complete(slug, id, result.state(), result.output());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException exception) { return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage())); }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(IllegalStateException exception) { return ResponseEntity.status(409).body(Map.of("error", exception.getMessage())); }
    public record Result(OperationAction.State state, String output) {}
}
