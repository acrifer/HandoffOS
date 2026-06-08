-- AI knowledge control-plane tables for Dify-backed handoff Skill features.

CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_url VARCHAR(500),
    dify_document_id VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'UPLOADED',
    summary TEXT,
    raw_content TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES handoff_skill(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_knowledge_document_skill_time ON knowledge_document(skill_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_knowledge_document_dify_doc ON knowledge_document(dify_document_id);

CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT,
    source_title VARCHAR(255),
    source_locator VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES knowledge_document(id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES handoff_skill(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_document ON knowledge_chunk(document_id, chunk_index);
CREATE INDEX IF NOT EXISTS idx_knowledge_chunk_skill ON knowledge_chunk(skill_id, create_time DESC);

CREATE TABLE IF NOT EXISTS vector_index_mapping (
    id BIGSERIAL PRIMARY KEY,
    chunk_id BIGINT NOT NULL,
    dify_dataset_id VARCHAR(128),
    dify_document_id VARCHAR(128),
    embedding_model VARCHAR(80),
    index_status VARCHAR(32) DEFAULT 'PENDING',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (chunk_id) REFERENCES knowledge_chunk(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vector_mapping_chunk ON vector_index_mapping(chunk_id);
CREATE INDEX IF NOT EXISTS idx_vector_mapping_dify_doc ON vector_index_mapping(dify_document_id);

CREATE TABLE IF NOT EXISTS ai_qa_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    answer TEXT,
    citations TEXT,
    conversation_id VARCHAR(128),
    dify_workflow_run_id VARCHAR(128),
    latency_ms INT,
    status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
    no_answer BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES handoff_skill(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ai_qa_log_user_skill_time ON ai_qa_log(user_id, skill_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_ai_qa_log_time ON ai_qa_log(create_time DESC);
CREATE INDEX IF NOT EXISTS idx_ai_qa_log_no_answer ON ai_qa_log(no_answer);

CREATE TABLE IF NOT EXISTS ai_feedback (
    id BIGSERIAL PRIMARY KEY,
    qa_log_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL,
    feedback_type VARCHAR(32),
    comment TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (qa_log_id) REFERENCES ai_qa_log(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES "user"(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ai_feedback_qa_time ON ai_feedback(qa_log_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_ai_feedback_time ON ai_feedback(create_time DESC);

CREATE TABLE IF NOT EXISTS prompt_template (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    scenario VARCHAR(50) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    content TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_prompt_template_scenario ON prompt_template(scenario, enabled, version DESC);

INSERT INTO prompt_template (name, scenario, version, content, enabled)
VALUES
('知识库问答 Prompt', 'KNOWLEDGE_QA', 1, '你是团队项目交接助手。请只基于以下知识库片段回答用户问题，不要编造未出现的信息。 Skill名称：{skill_name} 角色说明：{role_description} 用户问题：{question} 历史对话：{chat_history} 知识库片段：{retrieved_context} 回答要求：先给直接答案；涉及流程用步骤；涉及风险标注风险点；关键结论标注引用编号；资料不足时回答当前知识库没有足够信息。', TRUE),
('摘要生成 Prompt', 'SUMMARY', 1, '请将以下团队交接资料整理成结构化摘要。资料标题：{document_title} 资料来源：{source_type} 资料内容：{document_content} 输出核心背景、关键流程、角色与责任、决策规则、风险点和新人需要确认的问题。只总结资料中出现的信息。', TRUE),
('问题推荐 Prompt', 'QUESTION_RECOMMENDATION', 1, '请基于 Skill 摘要为新人推荐 8 个最值得提问的问题，覆盖流程、权限、风险、负责人、上线检查和常见故障，返回 JSON 数组。', TRUE),
('管理员日志分析 Prompt', 'ADMIN_LOG_ANALYSIS', 1, '你是 AI 知识库运营分析助手。请分析问答日志和用户反馈，找出高频问题、无答案问题、差评原因、建议补充的文档和 Prompt 优化建议。', TRUE)
ON CONFLICT DO NOTHING;
