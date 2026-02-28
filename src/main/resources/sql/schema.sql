-- ADD Memo System
CREATE TABLE memos (
    id BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255),
    content    TEXT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

ALTER TABLE memos
ADD CONSTRAINT uq_memos_name UNIQUE (name);


-- ADD Project View
CREATE TABLE game_projects (
    id BIGSERIAL PRIMARY KEY,
    game_url VARCHAR(512) NOT NULL,
    memo_id BIGINT,
    CONSTRAINT fk_game_projects_memo FOREIGN KEY (memo_id) REFERENCES memos(id) ON DELETE CASCADE
);


-- ADD Statistic System
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


-- ADD RAG
-- VECTOR DB
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS vector_store (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text,
    metadata json,
    embedding vector(1536)
);

CREATE INDEX ON vector_store USING HNSW (embedding vector_cosine_ops);

CREATE TABLE IF NOT EXISTS rag_documents (
    id  BIGSERIAL   PRIMARY KEY,

    resource_type   VARCHAR(10), -- memos
    resource_id BIGINT,

    content TEXT,

    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_rag_documents_resource_type_id ON rag_documents(resource_type, resource_id);

ALTER TABLE request_statistics ADD COLUMN ip VARCHAR(15);

-- ADD Search System
CREATE EXTENSION pg_trgm;


CREATE INDEX idx_content_trgm ON memos USING GIN (content gin_trgm_ops);