package com.lifeos.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HandoffSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void initialize() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS feishu_chat_binding (
                    id BIGSERIAL PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    chat_id VARCHAR(128) NOT NULL,
                    chat_name VARCHAR(255),
                    skill_id BIGINT NOT NULL,
                    enabled BOOLEAN NOT NULL DEFAULT TRUE,
                    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_feishu_chat_binding_chat ON feishu_chat_binding(chat_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_feishu_chat_binding_user ON feishu_chat_binding(user_id, create_time DESC)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_feishu_chat_binding_skill ON feishu_chat_binding(skill_id)");

        jdbcTemplate.execute("""
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
                    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_feishu_bot_event_chat_time ON feishu_bot_event(chat_id, create_time DESC)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_feishu_bot_event_status_time ON feishu_bot_event(status, create_time DESC)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_feishu_bot_event_message ON feishu_bot_event(message_id)");

        jdbcTemplate.execute("ALTER TABLE task ADD COLUMN IF NOT EXISTS skill_id BIGINT");
        jdbcTemplate.execute("ALTER TABLE task ADD COLUMN IF NOT EXISTS source_qa_log_id BIGINT");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_task_skill_id ON task(skill_id)");

        jdbcTemplate.execute("ALTER TABLE note ADD COLUMN IF NOT EXISTS review_state VARCHAR(32) DEFAULT 'NEW'");
        jdbcTemplate.execute("ALTER TABLE note ADD COLUMN IF NOT EXISTS next_review_at TIMESTAMP");
        jdbcTemplate.execute("ALTER TABLE note ADD COLUMN IF NOT EXISTS last_reviewed_at TIMESTAMP");
    }
}
