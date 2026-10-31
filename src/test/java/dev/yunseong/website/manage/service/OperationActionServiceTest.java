package dev.yunseong.website.manage.service;

import dev.yunseong.website.manage.domain.OperationAction;
import dev.yunseong.website.manage.domain.ProjectOverview;
import dev.yunseong.website.manage.domain.ProjectReportContent;
import dev.yunseong.website.manage.repository.OperationActionRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationActionServiceTest {
    @Mock OperationActionRepository actions;
    @Mock ProjectReportService reports;
    @InjectMocks OperationActionService service;

    @Test
    void enqueuesOnlyCataloguedConfirmedActionAndPreservesRequestedIdentity() {
        when(reports.find("palworld")).thenReturn(Optional.of(project(false)));
        when(actions.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OperationAction queued = service.enqueue("palworld", "start", "admin", false);

        assertEquals(OperationAction.State.QUEUED, queued.getState());
        assertEquals("palworld:start", queued.getActiveKey());
        assertEquals("admin", queued.getRequestedBy());
    }

    @Test
    void refusesMissingOrUnconfirmedActionBeforeItCreatesQueueEntry() {
        when(reports.find("palworld")).thenReturn(Optional.of(project(true)));
        assertThrows(IllegalArgumentException.class, () -> service.enqueue("palworld", "start", "admin", false));
        assertThrows(IllegalArgumentException.class, () -> service.enqueue("palworld", "unknown", "admin", true));
        verify(actions, never()).saveAndFlush(any());
    }

    @Test
    void claimsOldestQueuedActionAndExpiresRunningLeaseWithoutRetryingIt() {
        Instant now = Instant.now();
        OperationAction expired = new OperationAction("palworld", "start", "admin", now.minusSeconds(600));
        expired.claim(now.minusSeconds(600), now.minusSeconds(1));
        OperationAction queued = new OperationAction("palworld", "stop", "admin", now);
        when(actions.findExpiredRunningForUpdate(eq("palworld"), any())).thenReturn(List.of(expired));
        when(actions.findFirstByProjectSlugAndStateOrderByRequestedAtAscIdAsc("palworld", OperationAction.State.QUEUED))
                .thenReturn(Optional.of(queued));

        var claim = service.claim("palworld").orElseThrow();

        assertEquals(queued.getId(), claim.id());
        assertEquals("stop", claim.actionKey());
        assertEquals(OperationAction.State.RUNNING, queued.getState());
        assertEquals(OperationAction.State.FAILURE, expired.getState());
        assertEquals("Runner lease expired before a result was reported.", expired.getOutput());
        assertEquals(null, expired.getActiveKey());
    }

    @Test
    void rejectsOversizedOutputBeforeReadingOrTransitioningAction() {
        String output = "x".repeat(OperationActionService.MAX_OUTPUT_BYTES + 1);
        assertThrows(IllegalArgumentException.class,
                () -> service.complete("palworld", "id", OperationAction.State.SUCCESS, output));
        verify(actions, never()).findById(any());
    }

    @Test
    void acceptsOnlyTerminalRunnerResultsForRunningAction() {
        OperationAction running = new OperationAction("palworld", "start", "admin", Instant.now());
        running.claim(Instant.now(), Instant.now().plusSeconds(60));
        when(actions.findById(running.getId())).thenReturn(Optional.of(running));

        service.complete("palworld", running.getId(), OperationAction.State.SUCCESS, "started");

        assertEquals(OperationAction.State.SUCCESS, running.getState());
        assertFalse(running.getActiveKey() != null);
        assertThrows(IllegalArgumentException.class,
                () -> service.complete("palworld", running.getId(), OperationAction.State.QUEUED, "bad"));
    }

    private ProjectOverview project(boolean requiresConfirmation) {
        var action = new ProjectReportContent.Action("start", "Start", "Starts Palworld", requiresConfirmation);
        var content = new ProjectReportContent("Palworld", "runner", ProjectReportContent.Status.IDLE,
                "stopped", 5, List.of(), List.of(), List.of(action));
        return new ProjectOverview("palworld", content, Instant.now(), false);
    }
}
