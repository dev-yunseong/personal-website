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
    referer VARCHAR(1024),
    user_agent VARCHAR(512),
    created_at TIMESTAMP
);

CREATE INDEX idx_request_statistics_created_at ON request_statistics(created_at);
CREATE INDEX idx_request_statistics_uri ON request_statistics(uri);

-- VECTOR DB
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS rag_documents (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text,
    metadata json,
    embedding vector(1536), -- 1536 is the default embedding dimension
);

ALTER TABLE rag_documents DROP embedding;
ALTER TABLE rag_documents ADD COLUMN embedding vector(1536);
ALTER TABLE rag_documents ADD COLUMN updated_at TIMESTAMP DEFAULT now();

CREATE INDEX ON rag_documents USING HNSW (embedding vector_cosine_ops);
