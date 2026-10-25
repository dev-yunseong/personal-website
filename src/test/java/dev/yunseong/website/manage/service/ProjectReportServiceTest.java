package dev.yunseong.website.manage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.yunseong.website.manage.domain.ProjectReport;
import dev.yunseong.website.manage.domain.ProjectReportContent;
import dev.yunseong.website.manage.repository.ProjectReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectReportServiceTest {
    @Mock ProjectReportRepository reports;
    @InjectMocks ProjectReportService service;
    @org.mockito.Spy private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rejectsInvalidSlugOversizedBodyAndUnknownWidgetTypeWithoutSaving() {
        assertThrows(IllegalArgumentException.class, () -> service.publish("Bad Slug", json(validReport())));
        assertThrows(IllegalArgumentException.class, () -> service.publish("site", new byte[ProjectReportService.MAX_BYTES + 1]));
        assertThrows(IllegalArgumentException.class, () -> service.publish("site", "{\"name\":\"x\",\"agent\":\"x\",\"status\":\"RUNNING\",\"summary\":\"x\",\"staleAfterMinutes\":120,\"widgets\":[{\"type\":\"PIE\"}]}".getBytes()));
        verifyNoInteractions(reports);
    }

    @Test
    void rejectsMissingRequiredValuesAndInvalidWidgetValuesWithoutSaving() {
        var missingName = new ProjectReportContent(null, "agent", ProjectReportContent.Status.RUNNING,
                "summary", 120, List.of());
        var invalidLine = new ProjectReportContent("name", "agent", ProjectReportContent.Status.RUNNING,
                "summary", 120, List.of(new ProjectReportContent.Widget(ProjectReportContent.WidgetType.LINE,
                "trend", null, null, null, List.of(new ProjectReportContent.Point("now", Double.NaN)))));

        assertThrows(IllegalArgumentException.class, () -> service.publish("site", json(missingName)));
        assertThrows(IllegalArgumentException.class, () -> service.publish("site", json(invalidLine)));
        verifyNoInteractions(reports);
    }

    @Test
    void rejectedUpdateDoesNotReplacePreviouslySavedReport() {
        var existing = new ProjectReport("site", new String(json(validReport())), Instant.parse("2020-01-01T00:00:00Z"));
        when(reports.findById("site")).thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () -> service.publish("site", "null".getBytes()));

        verify(reports, never()).save(any(ProjectReport.class));
        var retained = service.find("site").orElseThrow();
        assertEquals("Project", retained.content().name());
        assertEquals(existing.getUpdatedAt(), retained.updatedAt());
    }

    @Test
    void savesAllowedWidgetsAsValidatedContentAndUsesSlugAsUpsertId() throws Exception {
        var content = validReport();
        service.publish("my-project", json(content));

        ArgumentCaptor<ProjectReport> saved = ArgumentCaptor.forClass(ProjectReport.class);
        verify(reports).save(saved.capture());
        assertEquals("my-project", saved.getValue().getSlug());
        assertEquals(content, objectMapper.readValue(saved.getValue().getPayload(), ProjectReportContent.class));
    }

    @Test
    void marksOldReportStaleAndFutureDatedReportFresh() {
        var payload = new String(json(new ProjectReportContent("name", "agent", ProjectReportContent.Status.RUNNING,
                "summary", 5, List.of())));
        when(reports.findById("old")).thenReturn(Optional.of(new ProjectReport("old", payload, Instant.parse("2020-01-01T00:00:00Z"))));
        when(reports.findById("future")).thenReturn(Optional.of(new ProjectReport("future", payload, Instant.parse("2999-01-01T00:00:00Z"))));

        assertTrue(service.find("old").orElseThrow().stale());
        assertFalse(service.find("future").orElseThrow().stale());
    }

    private ProjectReportContent validReport() {
        return new ProjectReportContent("Project", "codex", ProjectReportContent.Status.RUNNING, "summary", 120, List.of(
                new ProjectReportContent.Widget(ProjectReportContent.WidgetType.METRIC, "Tasks", "3 / 5", null, null, null),
                new ProjectReportContent.Widget(ProjectReportContent.WidgetType.TABLE, "Status", null,
                        List.of("Task", "State"), List.of(List.of("Login", "Done")), null),
                new ProjectReportContent.Widget(ProjectReportContent.WidgetType.LINE, "Trend", null, null, null,
                        List.of(new ProjectReportContent.Point("today", 3.0)))));
    }

    private byte[] json(ProjectReportContent content) {
        try {
            return objectMapper.writeValueAsBytes(content);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
