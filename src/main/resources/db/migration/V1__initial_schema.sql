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

-- improve statistics
ALTER TABLE request_statistics ADD COLUMN status_code SMALLINT;

-- ADD Mini App System
CREATE TABLE mini_apps (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    app_url VARCHAR(1024) NOT NULL
);

-- ADD Chat Conversation History
CREATE TABLE chat_conversations (
    id BIGSERIAL PRIMARY KEY,
    ip VARCHAR(45) NOT NULL,
    user_message TEXT NOT NULL,
    ai_response TEXT,
    tools_used VARCHAR(1024),
    created_at TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_chat_conversations_ip ON chat_conversations(ip);
CREATE INDEX idx_chat_conversations_created_at ON chat_conversations(created_at);

-- ADD Bot Flag And Response Time To Statistics
ALTER TABLE request_statistics ADD COLUMN is_bot BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE request_statistics ADD COLUMN duration_ms INTEGER;

CREATE INDEX idx_request_statistics_is_bot ON request_statistics(is_bot);

-- Backfill is_bot for rows written before the column existed. Keep the pattern in
-- sync with BotDetector; a missing User-Agent counts as a bot there too.
UPDATE request_statistics
SET is_bot = TRUE
WHERE user_agent IS NULL
   OR btrim(user_agent) = ''
   OR user_agent ~* 'bot|crawl|spider|slurp|curl|wget|python-requests|httpx|scrapy|okhttp|java/|go-http-client|libwww-perl|headlesschrome|facebookexternalhit|feedfetcher|monitoring|uptime';

-- ADD Country Code To Statistics
-- Resolved from ip at insert time, so reads are a plain GROUP BY country_code.
ALTER TABLE request_statistics ADD COLUMN country_code CHAR(2);

CREATE INDEX idx_request_statistics_country_code ON request_statistics(country_code);

-- Existing rows cannot be backfilled in SQL: the mapping lives in the MaxMind
-- database. After deploying, press "Backfill" on the console dashboard
-- Countries tab (POST /api/admin/console/geo/backfill). It is chunked and safe
-- to repeat; only rows whose country_code is still null are written.
