-- Dify/Feishu control-plane fields for team handoff Skill.

ALTER TABLE handoff_skill
    ADD COLUMN IF NOT EXISTS dify_dataset_id VARCHAR(128),
    ADD COLUMN IF NOT EXISTS last_sync_time TIMESTAMP;

ALTER TABLE handoff_skill_source
    ADD COLUMN IF NOT EXISTS source_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS dify_document_id VARCHAR(128),
    ADD COLUMN IF NOT EXISTS indexing_status VARCHAR(32),
    ADD COLUMN IF NOT EXISTS last_sync_time TIMESTAMP;

ALTER TABLE handoff_skill_chat
    ADD COLUMN IF NOT EXISTS dify_workflow_run_id VARCHAR(128);

ALTER TABLE ai_workflow_job
    ADD COLUMN IF NOT EXISTS dify_workflow_run_id VARCHAR(128);

CREATE INDEX IF NOT EXISTS idx_handoff_skill_dify_dataset ON handoff_skill(dify_dataset_id);
CREATE INDEX IF NOT EXISTS idx_handoff_skill_source_dify_document ON handoff_skill_source(dify_document_id);
CREATE INDEX IF NOT EXISTS idx_handoff_skill_source_source_hash ON handoff_skill_source(source_hash);
CREATE INDEX IF NOT EXISTS idx_ai_job_dify_workflow_run ON ai_workflow_job(dify_workflow_run_id);
