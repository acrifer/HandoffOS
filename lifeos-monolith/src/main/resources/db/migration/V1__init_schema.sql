-- LifeOS Database Schema with pgvector support
-- PostgreSQL 16+ with pgvector extension

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- User table
CREATE TABLE IF NOT EXISTS "user" (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_username ON "user"(username);

-- Note table (unified, no sharding)
CREATE TABLE IF NOT EXISTS note (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    content TEXT,
    tags VARCHAR(255),
    summary TEXT,
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE INDEX idx_note_user_id ON note(user_id);
CREATE INDEX idx_note_user_create_time ON note(user_id, create_time DESC);
CREATE INDEX idx_note_user_pinned_update ON note(user_id, pinned DESC, update_time DESC);

-- Note embeddings table (for RAG)
CREATE TABLE IF NOT EXISTS note_embedding (
    id BIGSERIAL PRIMARY KEY,
    note_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    embedding vector(1536),  -- OpenAI/DeepSeek embedding dimension
    embedding_model VARCHAR(50) NOT NULL DEFAULT 'text-embedding-3-small',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (note_id) REFERENCES note(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE INDEX idx_note_embedding_user_id ON note_embedding(user_id);
CREATE INDEX idx_note_embedding_vector ON note_embedding USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- Task table (merged from task-service)
CREATE TABLE IF NOT EXISTS task (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    deadline TIMESTAMP,
    tags VARCHAR(255),
    source_note_id BIGINT,
    status SMALLINT NOT NULL DEFAULT 0,  -- 0: pending, 1: completed
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    complete_time TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
    FOREIGN KEY (source_note_id) REFERENCES note(id) ON DELETE SET NULL
);

CREATE INDEX idx_task_user_id ON task(user_id);
CREATE INDEX idx_task_user_status ON task(user_id, status);
CREATE INDEX idx_task_source_note_id ON task(source_note_id);

-- AI workflow job table
CREATE TABLE IF NOT EXISTS ai_workflow_job (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    note_id BIGINT,
    skill_id BIGINT,
    job_type VARCHAR(32) NOT NULL,  -- SUMMARY, ORGANIZE, EXTRACT_TASKS, WEEKLY_REVIEW, SKILL_DISTILL, SKILL_ASK, RAG_QUERY
    status VARCHAR(20) NOT NULL,  -- PENDING, PROCESSING, SUCCESS, FAILED
    request_payload TEXT,
    result_payload TEXT,
    error_message VARCHAR(500),
    finished_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
    FOREIGN KEY (note_id) REFERENCES note(id) ON DELETE SET NULL
);

CREATE INDEX idx_ai_job_user_time ON ai_workflow_job(user_id, create_time DESC);
CREATE INDEX idx_ai_job_note_time ON ai_workflow_job(note_id, create_time DESC);
CREATE INDEX idx_ai_job_type_status ON ai_workflow_job(job_type, status);

-- Handoff skill table
CREATE TABLE IF NOT EXISTS handoff_skill (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    role_description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',  -- DRAFT, DISTILLED
    distill_result TEXT,
    source_count INT NOT NULL DEFAULT 0,
    document_source_count INT NOT NULL DEFAULT 0,
    chat_source_count INT NOT NULL DEFAULT 0,
    latest_job_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE INDEX idx_handoff_skill_user_time ON handoff_skill(user_id, update_time DESC);
CREATE INDEX idx_handoff_skill_status ON handoff_skill(status);

-- Handoff skill source table
CREATE TABLE IF NOT EXISTS handoff_skill_source (
    id BIGSERIAL PRIMARY KEY,
    skill_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    source_type VARCHAR(32) NOT NULL,  -- DOCUMENT, CHAT
    external_id VARCHAR(255) NOT NULL,
    title VARCHAR(255),
    content TEXT,
    content_hash VARCHAR(64) NOT NULL,
    source_time TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (skill_id) REFERENCES handoff_skill(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
    UNIQUE (skill_id, content_hash)
);

CREATE INDEX idx_handoff_skill_source_skill_time ON handoff_skill_source(skill_id, create_time DESC);
CREATE INDEX idx_handoff_skill_source_user ON handoff_skill_source(user_id);

-- Handoff skill chat table
CREATE TABLE IF NOT EXISTS handoff_skill_chat (
    id BIGSERIAL PRIMARY KEY,
    skill_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    job_id BIGINT,
    question TEXT NOT NULL,
    answer TEXT,
    citations TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (skill_id) REFERENCES handoff_skill(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
    FOREIGN KEY (job_id) REFERENCES ai_workflow_job(id) ON DELETE SET NULL
);

CREATE INDEX idx_handoff_skill_chat_skill_time ON handoff_skill_chat(skill_id, create_time DESC);
CREATE INDEX idx_handoff_skill_chat_user ON handoff_skill_chat(user_id);

-- Knowledge graph tables (for enhanced skill feature)
CREATE TABLE IF NOT EXISTS knowledge_entity (
    id BIGSERIAL PRIMARY KEY,
    skill_id BIGINT NOT NULL,
    entity_type VARCHAR(50) NOT NULL,  -- PERSON, PROJECT, PROCESS, CONCEPT
    entity_name VARCHAR(255) NOT NULL,
    description TEXT,
    confidence DECIMAL(3, 2) DEFAULT 0.5,  -- 0.0 to 1.0
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (skill_id) REFERENCES handoff_skill(id) ON DELETE CASCADE,
    UNIQUE (skill_id, entity_type, entity_name)
);

CREATE INDEX idx_knowledge_entity_skill ON knowledge_entity(skill_id);
CREATE INDEX idx_knowledge_entity_type ON knowledge_entity(entity_type);

CREATE TABLE IF NOT EXISTS knowledge_relation (
    id BIGSERIAL PRIMARY KEY,
    skill_id BIGINT NOT NULL,
    source_entity_id BIGINT NOT NULL,
    target_entity_id BIGINT NOT NULL,
    relation_type VARCHAR(50) NOT NULL,  -- RESPONSIBLE_FOR, DEPENDS_ON, PREREQUISITE, RELATED_TO
    confidence DECIMAL(3, 2) DEFAULT 0.5,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (skill_id) REFERENCES handoff_skill(id) ON DELETE CASCADE,
    FOREIGN KEY (source_entity_id) REFERENCES knowledge_entity(id) ON DELETE CASCADE,
    FOREIGN KEY (target_entity_id) REFERENCES knowledge_entity(id) ON DELETE CASCADE
);

CREATE INDEX idx_knowledge_relation_skill ON knowledge_relation(skill_id);
CREATE INDEX idx_knowledge_relation_source ON knowledge_relation(source_entity_id);
CREATE INDEX idx_knowledge_relation_target ON knowledge_relation(target_entity_id);

-- RAG query history table
CREATE TABLE IF NOT EXISTS rag_query_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    query TEXT NOT NULL,
    query_embedding vector(1536),
    answer TEXT,
    retrieved_note_ids BIGINT[],  -- Array of note IDs used in context
    model_used VARCHAR(50),
    response_time_ms INT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE INDEX idx_rag_query_user_time ON rag_query_history(user_id, create_time DESC);

-- Behavior event table (simplified, no separate service)
CREATE TABLE IF NOT EXISTS behavior_event (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,  -- NOTE_CREATED, NOTE_UPDATED, TASK_COMPLETED, AI_QUERY
    entity_type VARCHAR(50),  -- NOTE, TASK, AI_JOB
    entity_id BIGINT,
    metadata JSONB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE INDEX idx_behavior_event_user_time ON behavior_event(user_id, create_time DESC);
CREATE INDEX idx_behavior_event_type ON behavior_event(event_type);

-- Insert default admin user (password: Pass123456, BCrypt hashed)
INSERT INTO "user" (username, password, email, enabled)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'admin@lifeos.com', TRUE)
ON CONFLICT (username) DO NOTHING;

COMMENT ON TABLE note_embedding IS 'Stores vector embeddings for RAG-based semantic search';
COMMENT ON TABLE knowledge_entity IS 'Entities extracted from handoff skill sources';
COMMENT ON TABLE knowledge_relation IS 'Relations between entities in knowledge graph';
COMMENT ON TABLE rag_query_history IS 'History of RAG queries for analytics and improvement';
