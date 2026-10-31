package dev.yunseong.website.manage.service;

import dev.yunseong.website.manage.domain.OperationAction;
import dev.yunseong.website.manage.domain.ProjectReportContent;
import dev.yunseong.website.manage.repository.OperationActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperationActionService {
    public static final int MAX_OUTPUT_BYTES = 16 * 1024;
    private static final Duration LEASE = Duration.ofMinutes(5);

    private final OperationActionRepository actions;
    private final ProjectReportService reports;

    @Transactional
    public OperationAction enqueue(String projectSlug, String actionKey, String requestedBy,
                                   boolean confirmed) {
        ProjectReportContent.Action action = reports.find(projectSlug)
                .flatMap(project -> project.content().actions().stream()
                        .filter(item -> item.key().equals(actionKey))
                        .findFirst())
                .orElseThrow(() -> new IllegalArgumentException("등록된 action이 아닙니다."));
        if (action.requiresConfirmation() && !confirmed) {
            throw new IllegalArgumentException("이 action은 확인 후 실행할 수 있습니다.");
        }

        try {
            return actions.saveAndFlush(
                    new OperationAction(projectSlug, actionKey, requestedBy, Instant.now()));
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalStateException("이미 실행 중이거나 대기 중인 action입니다.");
        }
    }

    @Transactional
    public Optional<Claim> claim(String projectSlug) {
        Instant now = Instant.now();
        actions.findExpiredRunningForUpdate(projectSlug, now)
                .forEach(action -> action.expire(now));
        return actions.findFirstByProjectSlugAndStateOrderByRequestedAtAscIdAsc(
                        projectSlug, OperationAction.State.QUEUED)
                .map(action -> {
                    action.claim(now, now.plus(LEASE));
                    return new Claim(action.getId(), action.getProjectSlug(), action.getActionKey());
                });
    }

    @Transactional
    public void complete(String projectSlug, String id, OperationAction.State state, String output) {
        if (state != OperationAction.State.SUCCESS && state != OperationAction.State.FAILURE) {
            throw new IllegalArgumentException(
                    "result state는 SUCCESS 또는 FAILURE여야 합니다.");
        }
        if (output == null
                || output.getBytes(StandardCharsets.UTF_8).length > MAX_OUTPUT_BYTES) {
            throw new IllegalArgumentException("output은 UTF-8 16 KiB 이하여야 합니다.");
        }

        OperationAction action = actions.findById(id)
                .filter(found -> found.getProjectSlug().equals(projectSlug))
                .orElseThrow(() -> new IllegalArgumentException("action을 찾을 수 없습니다."));
        action.complete(state, output, Instant.now());
    }

    public List<OperationAction> recent(String projectSlug) {
        return actions.findTop20ByProjectSlugOrderByRequestedAtDesc(projectSlug);
    }

    public record Claim(String id, String projectSlug, String actionKey) {
    }
}
