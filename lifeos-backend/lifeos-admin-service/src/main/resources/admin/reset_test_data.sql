SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE `user_behavior`;
TRUNCATE TABLE `admin_audit_log`;
TRUNCATE TABLE `handoff_skill_chat`;
TRUNCATE TABLE `handoff_skill_source`;
TRUNCATE TABLE `handoff_skill`;
TRUNCATE TABLE `ai_workflow_job`;
TRUNCATE TABLE `task`;
TRUNCATE TABLE `note_0`;
TRUNCATE TABLE `note_1`;
TRUNCATE TABLE `note_2`;
TRUNCATE TABLE `note_3`;
TRUNCATE TABLE `user`;

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO `user` (`id`, `username`, `password`, `email`, `enabled`, `create_time`) VALUES
  (1, 'liwen_pm', 'Pass123456', 'liwen.pm@test.local', 1, '2026-03-01 09:10:00'),
  (2, 'zhouyi_dev', 'Pass123456', 'zhouyi.dev@test.local', 1, '2026-03-02 10:20:00'),
  (3, 'heqing_fit', 'Pass123456', 'heqing.fit@test.local', 1, '2026-03-02 21:05:00'),
  (4, 'susu_creator', 'Pass123456', 'susu.creator@test.local', 1, '2026-03-03 14:00:00'),
  (5, 'chenyu_grad', 'Pass123456', 'chenyu.grad@test.local', 1, '2026-03-04 08:30:00');
