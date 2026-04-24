ALTER TABLE `ai_workflow_job`
    ADD COLUMN `skill_id` bigint NULL DEFAULT NULL AFTER `note_id`,
    ADD INDEX `idx_ai_job_skill_time` (`skill_id`, `create_time`) USING BTREE,
    ADD INDEX `idx_ai_job_user_skill_time` (`user_id`, `skill_id`, `create_time`) USING BTREE;

CREATE TABLE IF NOT EXISTS `handoff_skill` (
    `id` bigint NOT NULL,
    `user_id` bigint NOT NULL,
    `name` varchar(100) NOT NULL,
    `role_description` text NULL,
    `status` varchar(32) NOT NULL DEFAULT 'DRAFT',
    `distill_result` longtext NULL,
    `source_count` int NOT NULL DEFAULT 0,
    `document_source_count` int NOT NULL DEFAULT 0,
    `chat_source_count` int NOT NULL DEFAULT 0,
    `latest_job_id` bigint NULL DEFAULT NULL,
    `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_handoff_skill_user_time` (`user_id`, `update_time`) USING BTREE,
    INDEX `idx_handoff_skill_status` (`status`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

CREATE TABLE IF NOT EXISTS `handoff_skill_source` (
    `id` bigint NOT NULL,
    `skill_id` bigint NOT NULL,
    `user_id` bigint NOT NULL,
    `source_type` varchar(32) NOT NULL,
    `external_id` varchar(255) NOT NULL,
    `title` varchar(255) NULL DEFAULT NULL,
    `content` longtext NULL,
    `content_hash` varchar(64) NOT NULL,
    `source_time` datetime NULL DEFAULT NULL,
    `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_handoff_skill_source_hash` (`skill_id`, `content_hash`) USING BTREE,
    INDEX `idx_handoff_skill_source_skill_time` (`skill_id`, `create_time`) USING BTREE,
    INDEX `idx_handoff_skill_source_user` (`user_id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

CREATE TABLE IF NOT EXISTS `handoff_skill_chat` (
    `id` bigint NOT NULL,
    `skill_id` bigint NOT NULL,
    `user_id` bigint NOT NULL,
    `job_id` bigint NULL DEFAULT NULL,
    `question` text NOT NULL,
    `answer` longtext NULL,
    `citations` longtext NULL,
    `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_handoff_skill_chat_skill_time` (`skill_id`, `create_time`) USING BTREE,
    INDEX `idx_handoff_skill_chat_user` (`user_id`) USING BTREE,
    INDEX `idx_handoff_skill_chat_job` (`job_id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;
