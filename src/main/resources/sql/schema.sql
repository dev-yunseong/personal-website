CREATE TABLE memos (
    id BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255),
    content    TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

ALTER TABLE memos
ADD CONSTRAINT uq_memos_name UNIQUE (name);

CREATE TABLE game_projects (
    id BIGSERIAL PRIMARY KEY,
    game_url VARCHAR(512) NOT NULL,
    memo_id BIGINT,
    CONSTRAINT fk_game_projects_memo FOREIGN KEY (memo_id) REFERENCES memos(id) ON DELETE CASCADE
);

CREATE TABLE request_statistics (
    id BIGSERIAL PRIMARY KEY,
    uri VARCHAR(512) NOT NULL,
    method VARCHAR(10) NOT NULL,
    request_count BIGINT NOT NULL,
    referer VARCHAR(1024),
    user_agent VARCHAR(512),
    created_at TIMESTAMP
);

CREATE INDEX idx_request_statistics_created_at ON request_statistics(created_at);
CREATE INDEX idx_request_statistics_uri ON request_statistics(uri);

