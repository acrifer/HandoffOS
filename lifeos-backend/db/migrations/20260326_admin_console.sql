-- For existing databases, add `user.enabled` separately if missing:
-- ALTER TABLE `user` ADD COLUMN `enabled` tinyint(1) NOT NULL DEFAULT 1 AFTER `email`;

CREATE TABLE IF NOT EXISTS `admin_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(100) NOT NULL,
  `display_name` varchar(100) NOT NULL,
  `email` varchar(100) NULL DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `last_login_time` datetime NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_username` (`username`)
);

CREATE TABLE IF NOT EXISTS `admin_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_code` varchar(50) NOT NULL,
  `role_name` varchar(100) NOT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_role_code` (`role_code`)
);

CREATE TABLE IF NOT EXISTS `admin_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_user_role` (`admin_user_id`, `role_id`),
  KEY `idx_admin_user_role_user` (`admin_user_id`),
  KEY `idx_admin_user_role_role` (`role_id`)
);

CREATE TABLE IF NOT EXISTS `admin_audit_log` (
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
  PRIMARY KEY (`id`),
  KEY `idx_admin_audit_time` (`create_time`),
  KEY `idx_admin_audit_user_time` (`admin_user_id`, `create_time`),
  KEY `idx_admin_audit_action_time` (`action`, `create_time`)
);
