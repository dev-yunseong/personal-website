CREATE TABLE operation_actions (
    id VARCHAR(36) PRIMARY KEY,
    project_slug VARCHAR(64) NOT NULL,
    action_key VARCHAR(64) NOT NULL,
    requested_by VARCHAR(100) NOT NULL,
    state VARCHAR(16) NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    lease_expires_at TIMESTAMP WITH TIME ZONE,
    active_key VARCHAR(129) UNIQUE,
    output TEXT
);
CREATE INDEX idx_operation_actions_claim ON operation_actions (project_slug, state, requested_at, id);
CREATE INDEX idx_operation_actions_recent ON operation_actions (project_slug, requested_at DESC);
