SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `note_0`;
CREATE TABLE `note_0` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `title` varchar(100) NOT NULL,
  `content` text NULL,
  `tags` varchar(255) NULL DEFAULT NULL,
  `summary` text NULL,
  `pinned` tinyint(1) NOT NULL DEFAULT 0,
  `review_state` varchar(20) NOT NULL DEFAULT 'NEW',
  `next_review_at` datetime NULL DEFAULT NULL,
  `last_reviewed_at` datetime NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_review_state`(`review_state` ASC) USING BTREE,
  INDEX `idx_next_review_at`(`next_review_at` ASC) USING BTREE,
  INDEX `idx_user_create_time`(`user_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_user_pinned_update`(`user_id` ASC, `pinned` ASC, `update_time` ASC) USING BTREE,
  INDEX `idx_user_pinned_create`(`user_id` ASC, `pinned` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_user_review_next`(`user_id` ASC, `review_state` ASC, `next_review_at` ASC) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `note_1`;
CREATE TABLE `note_1` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `title` varchar(100) NOT NULL,
  `content` text NULL,
  `tags` varchar(255) NULL DEFAULT NULL,
  `summary` text NULL,
  `pinned` tinyint(1) NOT NULL DEFAULT 0,
  `review_state` varchar(20) NOT NULL DEFAULT 'NEW',
  `next_review_at` datetime NULL DEFAULT NULL,
  `last_reviewed_at` datetime NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_review_state`(`review_state` ASC) USING BTREE,
  INDEX `idx_next_review_at`(`next_review_at` ASC) USING BTREE,
  INDEX `idx_user_create_time`(`user_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_user_pinned_update`(`user_id` ASC, `pinned` ASC, `update_time` ASC) USING BTREE,
  INDEX `idx_user_pinned_create`(`user_id` ASC, `pinned` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_user_review_next`(`user_id` ASC, `review_state` ASC, `next_review_at` ASC) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `note_2`;
CREATE TABLE `note_2` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `title` varchar(100) NOT NULL,
  `content` text NULL,
  `tags` varchar(255) NULL DEFAULT NULL,
  `summary` text NULL,
  `pinned` tinyint(1) NOT NULL DEFAULT 0,
  `review_state` varchar(20) NOT NULL DEFAULT 'NEW',
  `next_review_at` datetime NULL DEFAULT NULL,
  `last_reviewed_at` datetime NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_review_state`(`review_state` ASC) USING BTREE,
  INDEX `idx_next_review_at`(`next_review_at` ASC) USING BTREE,
  INDEX `idx_user_create_time`(`user_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_user_pinned_update`(`user_id` ASC, `pinned` ASC, `update_time` ASC) USING BTREE,
  INDEX `idx_user_pinned_create`(`user_id` ASC, `pinned` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_user_review_next`(`user_id` ASC, `review_state` ASC, `next_review_at` ASC) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `note_3`;
CREATE TABLE `note_3` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `title` varchar(100) NOT NULL,
  `content` text NULL,
  `tags` varchar(255) NULL DEFAULT NULL,
  `summary` text NULL,
  `pinned` tinyint(1) NOT NULL DEFAULT 0,
  `review_state` varchar(20) NOT NULL DEFAULT 'NEW',
  `next_review_at` datetime NULL DEFAULT NULL,
  `last_reviewed_at` datetime NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_review_state`(`review_state` ASC) USING BTREE,
  INDEX `idx_next_review_at`(`next_review_at` ASC) USING BTREE,
  INDEX `idx_user_create_time`(`user_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_user_pinned_update`(`user_id` ASC, `pinned` ASC, `update_time` ASC) USING BTREE,
  INDEX `idx_user_pinned_create`(`user_id` ASC, `pinned` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_user_review_next`(`user_id` ASC, `review_state` ASC, `next_review_at` ASC) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `task`;
CREATE TABLE `task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(100) NOT NULL,
  `description` text NULL,
  `deadline` datetime NULL DEFAULT NULL,
  `tags` varchar(255) NULL DEFAULT NULL,
  `source_note_id` bigint NULL DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 0,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_source_note_id`(`source_note_id` ASC) USING BTREE,
  INDEX `idx_task_user_create_time`(`user_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_task_user_status`(`user_id` ASC, `status` ASC) USING BTREE,
  INDEX `idx_task_user_status_source_note`(`user_id` ASC, `status` ASC, `source_note_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `ai_workflow_job`;
CREATE TABLE `ai_workflow_job` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `note_id` bigint NULL DEFAULT NULL,
  `job_type` varchar(32) NOT NULL,
  `status` varchar(20) NOT NULL,
  `request_payload` longtext NULL,
  `result_payload` longtext NULL,
  `error_message` varchar(500) NULL DEFAULT NULL,
  `finished_time` datetime NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_job_user_time`(`user_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_ai_job_note_time`(`note_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_ai_job_type_status`(`job_type` ASC, `status` ASC) USING BTREE,
  INDEX `idx_ai_job_user_note_time`(`user_id` ASC, `note_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_ai_job_user_type_time`(`user_id` ASC, `job_type` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(100) NOT NULL,
  `email` varchar(100) NULL DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `admin_user`;
CREATE TABLE `admin_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(100) NOT NULL,
  `display_name` varchar(100) NOT NULL,
  `email` varchar(100) NULL DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `last_login_time` datetime NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_admin_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `admin_role`;
CREATE TABLE `admin_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_code` varchar(50) NOT NULL,
  `role_name` varchar(100) NOT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_admin_role_code`(`role_code` ASC) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `admin_user_role`;
CREATE TABLE `admin_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_admin_user_role`(`admin_user_id` ASC, `role_id` ASC) USING BTREE,
  INDEX `idx_admin_user_role_user`(`admin_user_id` ASC) USING BTREE,
  INDEX `idx_admin_user_role_role`(`role_id` ASC) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `admin_audit_log`;
CREATE TABLE `admin_audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_user_id` bigint NOT NULL,
  `admin_username` varchar(50) NOT NULL,
  `action` varchar(100) NOT NULL,
  `target_type` varchar(50) NOT NULL,
  `target_id` varchar(100) NULL DEFAULT NULL,
  `detail` text NULL,
  `success` tinyint(1) NOT NULL DEFAULT 1,
  `error_message` varchar(500) NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_admin_audit_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_admin_audit_user_time`(`admin_user_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_admin_audit_action_time`(`action` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

DROP TABLE IF EXISTS `user_behavior`;
CREATE TABLE `user_behavior` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL,
  `action_type` varchar(50) NOT NULL,
  `target_id` bigint NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_event_id`(`event_id` ASC) USING BTREE,
  INDEX `idx_user_id_action`(`user_id` ASC, `action_type` ASC) USING BTREE,
  INDEX `idx_user_id_time`(`user_id` ASC, `create_time` ASC) USING BTREE,
  INDEX `idx_user_id_action_time`(`user_id` ASC, `action_type` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
