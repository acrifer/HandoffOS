package com.lifeos.admin.service.impl;

import com.lifeos.admin.domain.vo.AdminConfigItemVO;
import com.lifeos.admin.domain.vo.AdminDashboardVO;
import com.lifeos.admin.domain.vo.AdminRecentIssueVO;
import com.lifeos.admin.domain.vo.AdminServiceStatusVO;
import com.lifeos.admin.domain.vo.AdminToolsVO;
import com.lifeos.admin.service.AdminAuditService;
import com.lifeos.admin.service.AdminOpsService;
import jakarta.annotation.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminOpsServiceImpl implements AdminOpsService {

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    // ==================== Redis 作用：测试环境状态清理 ====================
    // 运维模块里 Redis 不是业务缓存入口，而是“测试环境清理对象”之一。
    // resetTestData 时要把：
    // 1. 普通用户 token
    // 2. 管理员 token
    // 3. 登录限流计数
    // 一并清掉，确保重置后环境干净、可重复验证。
    // ================================================================

    @Resource
    private AdminAuditService adminAuditService;

    @Override
    public AdminDashboardVO getDashboard() {
        AdminDashboardVO dashboard = new AdminDashboardVO();
        dashboard.setTotalUsers(number("SELECT COUNT(*) FROM user"));
        dashboard.setTotalNotes(number("SELECT SUM(cnt) FROM (SELECT COUNT(*) cnt FROM note_0 UNION ALL SELECT COUNT(*) cnt FROM note_1 UNION ALL SELECT COUNT(*) cnt FROM note_2 UNION ALL SELECT COUNT(*) cnt FROM note_3) x"));
        dashboard.setTotalTasks(number("SELECT COUNT(*) FROM task"));
        dashboard.setFailedAiJobs(number("SELECT COUNT(*) FROM ai_workflow_job WHERE status = 'FAILED'"));
        List<AdminServiceStatusVO> services = getServices();
        dashboard.setTotalServices(services.size());
        dashboard.setOnlineServices(services.stream().filter(item -> "UP".equals(item.getStatus())).count());
        dashboard.setRecentIssues(recentIssues());
        return dashboard;
    }

    @Override
    public List<AdminServiceStatusVO> getServices() {
        List<ServiceTarget> targets = List.of(
                new ServiceTarget("MySQL", "infra", "mysql", 3306, url("127.0.0.1", envInt("MYSQL_PORT", 13306))),
                new ServiceTarget("Redis", "infra", "redis", 6379, url("127.0.0.1", envInt("REDIS_PORT", 6379))),
                new ServiceTarget("Nacos", "infra", "nacos", 8848, url("127.0.0.1", envInt("NACOS_PORT", 8848)) + "/nacos"),
                new ServiceTarget("RocketMQ NameSrv", "infra", "rocketmq-namesrv", 9876, null),
                new ServiceTarget("RocketMQ Broker", "infra", "rocketmq-broker", 10911, null),
                new ServiceTarget("User Service", "app", "lifeos-user-service", 8081, null),
                new ServiceTarget("Task Service", "app", "lifeos-task-service", 8082, null),
                new ServiceTarget("Note Service", "app", "lifeos-note-service", 8083, null),
                new ServiceTarget("AI Service", "app", "lifeos-ai-service", 8084, null),
                new ServiceTarget("Behavior Service", "app", "lifeos-behavior-service", 8085, null),
                new ServiceTarget("Gateway", "app", "lifeos-gateway", 8080, url("127.0.0.1", envInt("LIFEOS_GATEWAY_PORT", 8080))),
                new ServiceTarget("Admin Service", "app", "lifeos-admin-service", 8086, null));
        List<AdminServiceStatusVO> result = new ArrayList<>();
        for (ServiceTarget target : targets) {
            AdminServiceStatusVO item = new AdminServiceStatusVO();
            item.setName(target.name());
            item.setCategory(target.category());
            item.setHost(target.host());
            item.setPort(target.port());
            item.setAccessUrl(target.accessUrl());
            boolean up = reachable(target.host(), target.port());
            item.setStatus(up ? "UP" : "DOWN");
            item.setDetail(up ? "TCP reachable" : "TCP unreachable");
            result.add(item);
        }
        return result;
    }

    @Override
    public List<AdminConfigItemVO> getConfigs() {
        return List.of(
                config("MYSQL_JDBC_URL", env("MYSQL_JDBC_URL"), false),
                config("MYSQL_USERNAME", env("MYSQL_USERNAME", "root"), false),
                config("MYSQL_PASSWORD", env("MYSQL_PASSWORD"), true),
                config("NACOS_SERVER_ADDR", env("NACOS_SERVER_ADDR"), false),
                config("REDIS_HOST", env("REDIS_HOST"), false),
                config("REDIS_PORT", env("REDIS_PORT"), false),
                config("LIFEOS_JWT_SECRET", env("LIFEOS_JWT_SECRET"), true),
                config("LIFEOS_ADMIN_DEFAULT_USERNAME", env("LIFEOS_ADMIN_DEFAULT_USERNAME", "admin"), false),
                config("LIFEOS_ADMIN_DEFAULT_PASSWORD", env("LIFEOS_ADMIN_DEFAULT_PASSWORD"), true),
                config("LIFEOS_AI_BASE_URL", env("LIFEOS_AI_BASE_URL"), false),
                config("LIFEOS_AI_MODEL", env("LIFEOS_AI_MODEL"), false),
                config("LIFEOS_AI_API_KEY", env("LIFEOS_AI_API_KEY"), true));
    }

    @Override
    public AdminToolsVO getTools() {
        AdminToolsVO tools = new AdminToolsVO();
        tools.setFrontendUrl(url("127.0.0.1", envInt("LIFEOS_WEB_PORT", 5173)));
        tools.setAdminUrl(url("127.0.0.1", envInt("LIFEOS_ADMIN_WEB_PORT", 5174)));
        tools.setGatewayUrl(url("127.0.0.1", envInt("LIFEOS_GATEWAY_PORT", 8080)));
        tools.setSwaggerUrl(url("127.0.0.1", envInt("LIFEOS_GATEWAY_PORT", 8080)) + "/swagger-ui.html");
        tools.setNacosUrl(url("127.0.0.1", envInt("NACOS_PORT", 8848)) + "/nacos");
        tools.setLogCommands(List.of(
                "docker compose -f .\\docker-compose.full.yml ps",
                "docker logs --tail 200 lifeos-gateway",
                "docker logs --tail 200 lifeos-admin-service",
                "docker logs --tail 200 lifeos-note-service"));
        return tools;
    }

    @Override
    public void resetTestData(Long adminUserId, String adminUsername) {
        try {
            String sql = loadResetSql();
            executeSqlStatements(sql);
            jdbcTemplate.update("DELETE FROM admin_audit_log");
            // ===== Redis 批量清理 =====
            // 清理 Redis 中与认证和限流相关的 key，避免数据库重置后还残留旧会话状态。
            clear("token:*");
            clear("admin:token:*");
            clear("login:limit:*");
            adminAuditService.record(adminUserId, adminUsername, "RESET_TEST_DATA", "SYSTEM", "test-data", "Reset test data and auth cache", true, null);
        } catch (IOException | DataAccessException ex) {
            adminAuditService.record(adminUserId, adminUsername, "RESET_TEST_DATA", "SYSTEM", "test-data", "Reset test data and auth cache", false, ex.getMessage());
            throw new RuntimeException("Failed to reset test data", ex);
        }
    }

    private List<AdminRecentIssueVO> recentIssues() {
        List<AdminRecentIssueVO> issues = new ArrayList<>();
        issues.addAll(jdbcTemplate.query(
                "SELECT CONCAT(job_type, ' / user ', user_id) AS title, COALESCE(error_message, 'Unknown AI failure') AS message, update_time AS occurred_at FROM ai_workflow_job WHERE status = 'FAILED' ORDER BY update_time DESC LIMIT 5",
                (rs, rowNum) -> issue("AI_JOB", rs.getString("title"), rs.getString("message"), rs.getTimestamp("occurred_at"))));
        issues.addAll(jdbcTemplate.query(
                "SELECT action AS title, COALESCE(error_message, 'Admin action failed') AS message, create_time AS occurred_at FROM admin_audit_log WHERE success = 0 ORDER BY create_time DESC LIMIT 3",
                (rs, rowNum) -> issue("ADMIN_AUDIT", rs.getString("title"), rs.getString("message"), rs.getTimestamp("occurred_at"))));
        issues.sort((left, right) -> right.getOccurredAt().compareTo(left.getOccurredAt()));
        return issues.size() > 8 ? issues.subList(0, 8) : issues;
    }

    private AdminRecentIssueVO issue(String type, String title, String message, java.util.Date occurredAt) {
        AdminRecentIssueVO issue = new AdminRecentIssueVO();
        issue.setType(type);
        issue.setTitle(title);
        issue.setMessage(message);
        issue.setOccurredAt(occurredAt);
        return issue;
    }

    private AdminConfigItemVO config(String key, String value, boolean masked) {
        AdminConfigItemVO item = new AdminConfigItemVO();
        item.setKey(key);
        item.setValue(masked ? mask(value) : text(value));
        item.setMasked(masked);
        return item;
    }

    private long number(String sql) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0L : count;
    }

    private boolean reachable(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private void clear(String pattern) {
        // ===== Redis keys/delete =====
        // Redis 这里按模式批量删 key，用于测试环境恢复。
        // 只在后台运维手动触发时使用，不参与线上高频业务路径。
        var keys = stringRedisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    private String loadResetSql() throws IOException {
        String scriptPath = env("LIFEOS_RESET_SCRIPT_PATH");
        if (StringUtils.hasText(scriptPath) && new FileSystemResource(scriptPath).exists()) {
            return StreamUtils.copyToString(new FileSystemResource(scriptPath).getInputStream(), StandardCharsets.UTF_8);
        }
        return StreamUtils.copyToString(new ClassPathResource("admin/reset_test_data.sql").getInputStream(), StandardCharsets.UTF_8);
    }

    private void executeSqlStatements(String sql) {
        for (String statement : sql.split(";")) {
            String trimmed = statement.trim();
            if (!trimmed.isEmpty()) {
                jdbcTemplate.execute(trimmed);
            }
        }
    }

    private String env(String key) {
        return env(key, "");
    }

    private String env(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(key);
        }
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private int envInt(String key, int defaultValue) {
        return Integer.parseInt(env(key, String.valueOf(defaultValue)).trim());
    }

    private String mask(String value) {
        String text = text(value);
        if (text.length() <= 6) {
            return "******";
        }
        return text.substring(0, 3) + "******" + text.substring(text.length() - 3);
    }

    private String text(String value) {
        return value == null || value.isBlank() ? "(not set)" : value;
    }

    private String url(String host, int port) {
        return "http://" + host + ":" + port;
    }

    private record ServiceTarget(String name, String category, String host, int port, String accessUrl) {
    }
}
