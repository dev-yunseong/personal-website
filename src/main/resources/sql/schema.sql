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
    favicon_url VARCHAR(512),
    memo_id BIGINT,
    CONSTRAINT fk_game_projects_memo FOREIGN KEY (memo_id) REFERENCES memos(id) ON DELETE CASCADE
);

