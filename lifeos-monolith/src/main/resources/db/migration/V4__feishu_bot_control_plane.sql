-- Feishu bot control-plane tables and small workflow enrichments.

CREATE TABLE IF NOT EXISTS feishu_chat_binding (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    chat_id VARCHAR(128) NOT NULL,
    chat_name VARCHAR(255),
    skill_id BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES handoff_skill(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_feishu_chat_binding_chat ON feishu_chat_binding(chat_id);
CREATE INDEX IF NOT EXISTS idx_feishu_chat_binding_user ON feishu_chat_binding(user_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_feishu_chat_binding_skill ON feishu_chat_binding(skill_id);

CREATE TABLE IF NOT EXISTS feishu_bot_event (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(128) NOT NULL UNIQUE,
    message_id VARCHAR(128),
    chat_id VARCHAR(128) NOT NULL,
    sender_open_id VARCHAR(128),
    command_type VARCHAR(40),
    request_text TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'RECEIVED',
    error_message TEXT,
    job_id BIGINT,
    qa_log_id BIGINT,
    raw_payload TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (job_id) REFERENCES ai_workflow_job(id) ON DELETE SET NULL,
    FOREIGN KEY (qa_log_id) REFERENCES ai_qa_log(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_feishu_bot_event_chat_time ON feishu_bot_event(chat_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_feishu_bot_event_status_time ON feishu_bot_event(status, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_feishu_bot_event_message ON feishu_bot_event(message_id);

ALTER TABLE task ADD COLUMN IF NOT EXISTS skill_id BIGINT;
ALTER TABLE task ADD COLUMN IF NOT EXISTS source_qa_log_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_task_skill_id ON task(skill_id);

ALTER TABLE note ADD COLUMN IF NOT EXISTS review_state VARCHAR(32) DEFAULT 'NEW';
ALTER TABLE note ADD COLUMN IF NOT EXISTS next_review_at TIMESTAMP;
ALTER TABLE note ADD COLUMN IF NOT EXISTS last_reviewed_at TIMESTAMP;
