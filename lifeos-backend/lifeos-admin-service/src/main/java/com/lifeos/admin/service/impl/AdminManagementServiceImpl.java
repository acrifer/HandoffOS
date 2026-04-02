package com.lifeos.admin.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lifeos.admin.domain.dto.AdminNoteReviewStateDTO;
import com.lifeos.admin.domain.vo.AdminAiJobVO;
import com.lifeos.admin.domain.vo.AdminBehaviorVO;
import com.lifeos.admin.domain.vo.AdminNoteVO;
import com.lifeos.admin.domain.vo.AdminPageResult;
import com.lifeos.admin.domain.vo.AdminTaskVO;
import com.lifeos.admin.domain.vo.AdminUserVO;
import com.lifeos.api.ai.dto.AiAsyncJobCommand;
import com.lifeos.admin.service.AdminAuditService;
import com.lifeos.admin.service.AdminManagementService;
import com.lifeos.api.ai.dto.AiJobStatus;
import com.lifeos.api.ai.mq.AiMqConstants;
import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AdminManagementServiceImpl implements AdminManagementService {

    private static final String NOTE_UNION = """
            SELECT n.id, n.user_id, u.username, n.title, n.tags, n.summary, n.pinned, n.review_state, n.next_review_at, n.update_time
            FROM (
                SELECT id, user_id, title, tags, summary, pinned, review_state, next_review_at, update_time FROM note_0
                UNION ALL
                SELECT id, user_id, title, tags, summary, pinned, review_state, next_review_at, update_time FROM note_1
                UNION ALL
                SELECT id, user_id, title, tags, summary, pinned, review_state, next_review_at, update_time FROM note_2
                UNION ALL
                SELECT id, user_id, title, tags, summary, pinned, review_state, next_review_at, update_time FROM note_3
            ) n
            INNER JOIN user u ON u.id = n.user_id
            """;

    private static final String NOTE_TITLE_UNION = """
            SELECT id, user_id, title FROM note_0
            UNION ALL
            SELECT id, user_id, title FROM note_1
            UNION ALL
            SELECT id, user_id, title FROM note_2
            UNION ALL
            SELECT id, user_id, title FROM note_3
            """;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private AdminAuditService adminAuditService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    // ==================== Redis 作用：管理员强制下线用户 ====================
    // 后台管理这里碰 Redis 只有一个目的：
    // 管理员执行“禁用用户”时，立即踢掉该用户当前会话。
    // 也就是说，用户的 enabled 状态在数据库里改完后，
    // 还要顺手删除 token:*，否则已经登录的用户仍可能继续访问直到 token 自然过期。
    // ==================================================================

    @Autowired(required = false)
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public AdminPageResult<AdminUserVO> listUsers(int page, int size, String keyword) {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword.trim() + "%";
            where.append(" AND (username LIKE ? OR email LIKE ?) ");
            params.add(like);
            params.add(like);
        }
        long total = count("SELECT COUNT(*) FROM user" + where, params);
        List<Object> selectParams = new ArrayList<>(params);
        selectParams.add(size);
        selectParams.add(offset(page, size));
        List<AdminUserVO> records = jdbcTemplate.query(
                "SELECT id, username, email, enabled, create_time FROM user" + where
                        + " ORDER BY create_time DESC LIMIT ? OFFSET ?",
                BeanPropertyRowMapper.newInstance(AdminUserVO.class),
                selectParams.toArray());
        return page(records, total, page, size);
    }

    @Override
    public void updateUserEnabled(Long adminUserId, String adminUsername, Long userId, boolean enabled) {
        int updated = jdbcTemplate.update("UPDATE user SET enabled = ? WHERE id = ?", enabled, userId);
        if (updated == 0) {
            throw new RuntimeException("User not found");
        }
        if (!enabled) {
            // ===== Redis 删除：token:* =====
            // Redis 中删除当前用户会话，保证“禁用账号”是立即生效的，而不是只影响下次登录。
            stringRedisTemplate.delete("token:" + userId);
        }
        adminAuditService.record(adminUserId, adminUsername, enabled ? "ENABLE_USER" : "DISABLE_USER",
                "USER", String.valueOf(userId), "Updated account enabled status", true, null);
    }

    @Override
    public void resetUserPassword(Long adminUserId, String adminUsername, Long userId, String newPassword) {
        if (!StringUtils.hasText(newPassword) || newPassword.trim().length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters");
        }
        int updated = jdbcTemplate.update(
                "UPDATE user SET password = ? WHERE id = ?",
                passwordEncoder.encode(newPassword.trim()),
                userId);
        if (updated == 0) {
            throw new RuntimeException("User not found");
        }
        adminAuditService.record(adminUserId, adminUsername, "RESET_USER_PASSWORD",
                "USER", String.valueOf(userId), "Reset user password", true, null);
    }

    @Override
    public AdminPageResult<AdminNoteVO> listNotes(int page, int size, String keyword, String reviewState) {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword.trim() + "%";
            where.append(" AND (n.title LIKE ? OR n.tags LIKE ? OR n.summary LIKE ? OR u.username LIKE ?) ");
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (StringUtils.hasText(reviewState)) {
            where.append(" AND n.review_state = ? ");
            params.add(reviewState.trim().toUpperCase(Locale.ROOT));
        }
        long total = count("SELECT COUNT(*) FROM (" + NOTE_UNION + where + ") x", params);
        List<Object> selectParams = new ArrayList<>(params);
        selectParams.add(size);
        selectParams.add(offset(page, size));
        List<AdminNoteVO> records = jdbcTemplate.query(
                NOTE_UNION + where + " ORDER BY n.update_time DESC LIMIT ? OFFSET ?",
                BeanPropertyRowMapper.newInstance(AdminNoteVO.class),
                selectParams.toArray());
        return page(records, total, page, size);
    }

    @Override
    public void deleteNote(Long adminUserId, String adminUsername, Long noteId) {
        NoteLocator note = findNote(noteId);
        if (jdbcTemplate.update("DELETE FROM " + note.tableName() + " WHERE id = ?", noteId) == 0) {
            throw new RuntimeException("Note not found");
        }
        adminAuditService.record(adminUserId, adminUsername, "DELETE_NOTE",
                "NOTE", String.valueOf(noteId), "Deleted note owned by user " + note.userId(), true, null);
    }

    @Override
    public void updateNoteReview(Long adminUserId, String adminUsername, Long noteId, AdminNoteReviewStateDTO request) {
        if (request == null || !StringUtils.hasText(request.getReviewState())) {
            throw new RuntimeException("Review state is required");
        }
        NoteLocator note = findNote(noteId);
        if (jdbcTemplate.update(
                "UPDATE " + note.tableName() + " SET review_state = ?, next_review_at = ?, last_reviewed_at = NOW() WHERE id = ?",
                request.getReviewState().trim().toUpperCase(Locale.ROOT),
                request.getNextReviewAt(),
                noteId) == 0) {
            throw new RuntimeException("Note not found");
        }
        adminAuditService.record(adminUserId, adminUsername, "UPDATE_NOTE_REVIEW",
                "NOTE", String.valueOf(noteId), "Forced note review update", true, null);
    }

    @Override
    public AdminPageResult<AdminTaskVO> listTasks(int page, int size, String keyword, Integer status) {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword.trim() + "%";
            where.append(" AND (t.title LIKE ? OR u.username LIKE ?) ");
            params.add(like);
            params.add(like);
        }
        if (status != null) {
            where.append(" AND t.status = ? ");
            params.add(status);
        }
        String from = " FROM task t INNER JOIN user u ON u.id = t.user_id ";
        long total = count("SELECT COUNT(*)" + from + where, params);
        List<Object> selectParams = new ArrayList<>(params);
        selectParams.add(size);
        selectParams.add(offset(page, size));
        List<AdminTaskVO> records = jdbcTemplate.query(
                "SELECT t.id, t.user_id, u.username, t.title, t.status, t.deadline, t.source_note_id, t.create_time"
                        + from + where + " ORDER BY t.create_time DESC LIMIT ? OFFSET ?",
                BeanPropertyRowMapper.newInstance(AdminTaskVO.class),
                selectParams.toArray());
        return page(records, total, page, size);
    }

    @Override
    public void updateTaskStatus(Long adminUserId, String adminUsername, Long taskId, Integer status) {
        if (status == null || status < 0 || status > 2) {
            throw new RuntimeException("Invalid task status");
        }
        if (jdbcTemplate.update("UPDATE task SET status = ? WHERE id = ?", status, taskId) == 0) {
            throw new RuntimeException("Task not found");
        }
        adminAuditService.record(adminUserId, adminUsername, "UPDATE_TASK_STATUS",
                "TASK", String.valueOf(taskId), "Updated task status to " + status, true, null);
    }

    @Override
    public void deleteTask(Long adminUserId, String adminUsername, Long taskId) {
        if (jdbcTemplate.update("DELETE FROM task WHERE id = ?", taskId) == 0) {
            throw new RuntimeException("Task not found");
        }
        adminAuditService.record(adminUserId, adminUsername, "DELETE_TASK",
                "TASK", String.valueOf(taskId), "Deleted task", true, null);
    }

    @Override
    public AdminPageResult<AdminAiJobVO> listAiJobs(int page, int size, String keyword, String status, String jobType) {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword.trim() + "%";
            where.append(" AND (u.username LIKE ? OR COALESCE(n.title, '') LIKE ?) ");
            params.add(like);
            params.add(like);
        }
        if (StringUtils.hasText(status)) {
            where.append(" AND j.status = ? ");
            params.add(status.trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.hasText(jobType)) {
            where.append(" AND j.job_type = ? ");
            params.add(jobType.trim().toUpperCase(Locale.ROOT));
        }
        String from = """
                FROM ai_workflow_job j
                INNER JOIN user u ON u.id = j.user_id
                LEFT JOIN (
                """ + NOTE_TITLE_UNION + """
                ) n ON n.id = j.note_id AND n.user_id = j.user_id
                """;
        long total = count("SELECT COUNT(*) " + from + where, params);
        List<Object> selectParams = new ArrayList<>(params);
        selectParams.add(size);
        selectParams.add(offset(page, size));
        List<AdminAiJobVO> records = jdbcTemplate.query(
                "SELECT j.id, j.user_id, u.username, j.note_id, n.title AS note_title, j.job_type, j.status, j.error_message, j.create_time, j.finished_time "
                        + from + where + " ORDER BY j.create_time DESC LIMIT ? OFFSET ?",
                BeanPropertyRowMapper.newInstance(AdminAiJobVO.class),
                selectParams.toArray());
        return page(records, total, page, size);
    }

    @Override
    public void retryAiJob(Long adminUserId, String adminUsername, Long jobId) {
        if (rocketMQTemplate == null) {
            throw new RuntimeException("RocketMQ producer is not available");
        }
        AiJobRetrySource source = jdbcTemplate.query(
                "SELECT id, user_id, note_id, job_type, request_payload FROM ai_workflow_job WHERE id = ?",
                rs -> rs.next() ? new AiJobRetrySource(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getObject("note_id", Long.class),
                        rs.getString("job_type"),
                        rs.getString("request_payload")) : null,
                jobId);
        if (source == null) {
            throw new RuntimeException("AI job not found");
        }
        AiAsyncJobCommand command = buildRetryCommand(source);
        long newJobId = command.getJobId();
        String payload = com.alibaba.fastjson2.JSON.toJSONString(command);
        jdbcTemplate.update(
                "INSERT INTO ai_workflow_job (id, user_id, note_id, job_type, status, request_payload, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                newJobId, source.userId(), source.noteId(), source.jobType(), AiJobStatus.PENDING, payload);
        try {
            rocketMQTemplate.syncSend(AiMqConstants.TOPIC, payload, AiMqConstants.PRODUCER_TIMEOUT_MS);
            adminAuditService.record(adminUserId, adminUsername, "RETRY_AI_JOB",
                    "AI_JOB", String.valueOf(jobId), "Created replacement ai job " + newJobId, true, null);
        } catch (Exception ex) {
            jdbcTemplate.update(
                    "UPDATE ai_workflow_job SET status = ?, error_message = ?, finished_time = NOW(), update_time = NOW() WHERE id = ?",
                    AiJobStatus.FAILED,
                    "Failed to enqueue retried AI job",
                    newJobId);
            adminAuditService.record(adminUserId, adminUsername, "RETRY_AI_JOB",
                    "AI_JOB", String.valueOf(jobId), "Failed to enqueue replacement ai job " + newJobId, false, ex.getMessage());
            throw new RuntimeException("Failed to enqueue retried AI job", ex);
        }
    }

    @Override
    public void cancelAiJob(Long adminUserId, String adminUsername, Long jobId) {
        int updated = jdbcTemplate.update(
                "UPDATE ai_workflow_job SET status = ?, error_message = ?, finished_time = NOW(), update_time = NOW() WHERE id = ? AND status IN (?, ?)",
                "CANCELLED", "Cancelled by admin", jobId, AiJobStatus.PENDING, AiJobStatus.PROCESSING);
        if (updated == 0) {
            throw new RuntimeException("Only pending or processing jobs can be cancelled");
        }
        adminAuditService.record(adminUserId, adminUsername, "CANCEL_AI_JOB",
                "AI_JOB", String.valueOf(jobId), "Cancelled ai job", true, null);
    }

    @Override
    public AdminPageResult<AdminBehaviorVO> listBehaviors(int page, int size, String keyword, String actionType) {
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword.trim() + "%";
            where.append(" AND (u.username LIKE ? OR b.event_id LIKE ?) ");
            params.add(like);
            params.add(like);
        }
        if (StringUtils.hasText(actionType)) {
            where.append(" AND b.action_type = ? ");
            params.add(actionType.trim().toUpperCase(Locale.ROOT));
        }
        String from = " FROM user_behavior b INNER JOIN user u ON u.id = b.user_id ";
        long total = count("SELECT COUNT(*)" + from + where, params);
        List<Object> selectParams = new ArrayList<>(params);
        selectParams.add(size);
        selectParams.add(offset(page, size));
        List<AdminBehaviorVO> records = jdbcTemplate.query(
                "SELECT b.id, b.event_id, b.user_id, u.username, b.action_type, b.target_id, b.create_time "
                        + from + where + " ORDER BY b.create_time DESC LIMIT ? OFFSET ?",
                BeanPropertyRowMapper.newInstance(AdminBehaviorVO.class),
                selectParams.toArray());
        return page(records, total, page, size);
    }

    private NoteLocator findNote(Long noteId) {
        for (int shard = 0; shard < 4; shard++) {
            String table = "note_" + shard;
            NoteLocator found = jdbcTemplate.query(
                    "SELECT user_id FROM " + table + " WHERE id = ?",
                    rs -> rs.next() ? new NoteLocator(table, rs.getLong("user_id")) : null,
                    noteId);
            if (found != null) {
                return found;
            }
        }
        throw new RuntimeException("Note not found");
    }

    private AiAsyncJobCommand buildRetryCommand(AiJobRetrySource source) {
        AiAsyncJobCommand existing = com.alibaba.fastjson2.JSON.parseObject(source.requestPayload(), AiAsyncJobCommand.class);
        AiAsyncJobCommand command = existing == null ? new AiAsyncJobCommand() : existing;
        command.setJobId(IdWorker.getId());
        command.setUserId(source.userId());
        command.setNoteId(source.noteId());
        command.setJobType(source.jobType());
        return command;
    }

    private long count(String sql, List<Object> params) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class, params.toArray());
        return count == null ? 0L : count;
    }

    private int offset(int page, int size) {
        return Math.max(page - 1, 0) * size;
    }

    private <T> AdminPageResult<T> page(List<T> records, long total, int page, int size) {
        AdminPageResult<T> result = new AdminPageResult<>();
        result.setRecords(records);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        return result;
    }

    private record NoteLocator(String tableName, long userId) {
    }

    private record AiJobRetrySource(long id, long userId, Long noteId, String jobType, String requestPayload) {
    }
}
