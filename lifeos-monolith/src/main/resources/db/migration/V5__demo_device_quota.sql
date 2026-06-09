-- Device-scoped demo sessions and AI token quota controls.

CREATE TABLE IF NOT EXISTS demo_device_session (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(120) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL UNIQUE,
    device_name VARCHAR(100),
    user_agent TEXT,
    last_seen_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_demo_device_session_user ON demo_device_session(user_id);
CREATE INDEX IF NOT EXISTS idx_demo_device_session_seen ON demo_device_session(last_seen_at DESC);

CREATE TABLE IF NOT EXISTS demo_device_quota (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(120) NOT NULL UNIQUE,
    display_name VARCHAR(100),
    quota_limit BIGINT NOT NULL DEFAULT 100000,
    quota_used BIGINT NOT NULL DEFAULT 0,
    whitelist_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    period_start TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    period_end TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_demo_device_quota_enabled ON demo_device_quota(enabled);
CREATE INDEX IF NOT EXISTS idx_demo_device_quota_whitelist ON demo_device_quota(whitelist_enabled);

CREATE TABLE IF NOT EXISTS ai_token_usage_log (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(120) NOT NULL,
    user_id BIGINT NOT NULL,
    skill_id BIGINT,
    source_type VARCHAR(50),
    operation_type VARCHAR(50) NOT NULL,
    request_tokens BIGINT NOT NULL DEFAULT 0,
    response_tokens BIGINT NOT NULL DEFAULT 0,
    estimated BOOLEAN NOT NULL DEFAULT TRUE,
    external_run_id VARCHAR(120),
    status VARCHAR(20) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ai_token_usage_device_time ON ai_token_usage_log(device_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_ai_token_usage_user_time ON ai_token_usage_log(user_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_ai_token_usage_operation ON ai_token_usage_log(operation_type, status);
