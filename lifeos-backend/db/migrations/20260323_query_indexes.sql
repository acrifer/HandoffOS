ALTER TABLE `note_0`
    ADD INDEX `idx_user_create_time` (`user_id`, `create_time`),
    ADD INDEX `idx_user_pinned_update` (`user_id`, `pinned`, `update_time`),
    ADD INDEX `idx_user_pinned_create` (`user_id`, `pinned`, `create_time`),
    ADD INDEX `idx_user_review_next` (`user_id`, `review_state`, `next_review_at`);

ALTER TABLE `note_1`
    ADD INDEX `idx_user_create_time` (`user_id`, `create_time`),
    ADD INDEX `idx_user_pinned_update` (`user_id`, `pinned`, `update_time`),
    ADD INDEX `idx_user_pinned_create` (`user_id`, `pinned`, `create_time`),
    ADD INDEX `idx_user_review_next` (`user_id`, `review_state`, `next_review_at`);

ALTER TABLE `note_2`
    ADD INDEX `idx_user_create_time` (`user_id`, `create_time`),
    ADD INDEX `idx_user_pinned_update` (`user_id`, `pinned`, `update_time`),
    ADD INDEX `idx_user_pinned_create` (`user_id`, `pinned`, `create_time`),
    ADD INDEX `idx_user_review_next` (`user_id`, `review_state`, `next_review_at`);

ALTER TABLE `note_3`
    ADD INDEX `idx_user_create_time` (`user_id`, `create_time`),
    ADD INDEX `idx_user_pinned_update` (`user_id`, `pinned`, `update_time`),
    ADD INDEX `idx_user_pinned_create` (`user_id`, `pinned`, `create_time`),
    ADD INDEX `idx_user_review_next` (`user_id`, `review_state`, `next_review_at`);

ALTER TABLE `task`
    ADD INDEX `idx_task_user_create_time` (`user_id`, `create_time`),
    ADD INDEX `idx_task_user_status` (`user_id`, `status`),
    ADD INDEX `idx_task_user_status_source_note` (`user_id`, `status`, `source_note_id`);

ALTER TABLE `ai_workflow_job`
    ADD INDEX `idx_ai_job_user_note_time` (`user_id`, `note_id`, `create_time`),
    ADD INDEX `idx_ai_job_user_type_time` (`user_id`, `job_type`, `create_time`);

ALTER TABLE `user_behavior`
    ADD INDEX `idx_user_id_time` (`user_id`, `create_time`),
    ADD INDEX `idx_user_id_action_time` (`user_id`, `action_type`, `create_time`);
