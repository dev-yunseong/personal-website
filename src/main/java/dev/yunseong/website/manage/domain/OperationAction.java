package dev.yunseong.website.manage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "operation_actions")
@Getter
@NoArgsConstructor
public class OperationAction {
    public enum State { QUEUED, RUNNING, SUCCESS, FAILURE }

    @Id
    @Column(length = 36)
    private String id;
    @Column(nullable = false, length = 64)
    private String projectSlug;
    @Column(nullable = false, length = 64)
    private String actionKey;
    @Column(nullable = false, length = 100)
    private String requestedBy;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private State state;
    @Column(nullable = false)
    private Instant requestedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant leaseExpiresAt;
    @Column(length = 129)
    private String activeKey;
    @Column(columnDefinition = "text")
    private String output;

    public OperationAction(String projectSlug, String actionKey, String requestedBy, Instant requestedAt) {
        this.id = UUID.randomUUID().toString();
        this.projectSlug = projectSlug;
        this.actionKey = actionKey;
        this.requestedBy = requestedBy;
        this.requestedAt = requestedAt;
        this.state = State.QUEUED;
        this.activeKey = projectSlug + ":" + actionKey;
    }

    public void claim(Instant now, Instant leaseExpiresAt) {
        if (state != State.QUEUED) throw new IllegalStateException("대기 중인 action만 실행할 수 있습니다.");
        state = State.RUNNING;
        startedAt = now;
        this.leaseExpiresAt = leaseExpiresAt;
    }

    public void complete(State state, String output, Instant now) {
        if (this.state != State.RUNNING || (state != State.SUCCESS && state != State.FAILURE)) {
            throw new IllegalStateException("실행 중인 action만 완료할 수 있습니다.");
        }
        this.state = state;
        this.output = output;
        completedAt = now;
        leaseExpiresAt = null;
        activeKey = null;
    }

    public void expire(Instant now) {
        if (state == State.RUNNING) complete(State.FAILURE, "Runner lease expired before a result was reported.", now);
    }
}
