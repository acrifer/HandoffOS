package com.lifeos.demo.service;

import com.lifeos.config.JwtTokenUtil;
import com.lifeos.demo.DemoDeviceContext;
import com.lifeos.demo.dto.*;
import com.lifeos.demo.entity.AiTokenUsageLog;
import com.lifeos.demo.entity.DemoDeviceQuota;
import com.lifeos.demo.entity.DemoDeviceSession;
import com.lifeos.demo.exception.ApiException;
import com.lifeos.demo.repository.AiTokenUsageLogRepository;
import com.lifeos.demo.repository.DemoDeviceQuotaRepository;
import com.lifeos.demo.repository.DemoDeviceSessionRepository;
import com.lifeos.user.entity.User;
import com.lifeos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DemoDeviceService {

    private static final String QUOTA_EXCEEDED_MESSAGE = "当前设备 AI 额度已用完，请联系管理员调整白名单或额度";

    private final DemoDeviceSessionRepository sessionRepository;
    private final DemoDeviceQuotaRepository quotaRepository;
    private final AiTokenUsageLogRepository usageLogRepository;
    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;

    @Value("${demo.device.default-token-limit:100000}")
    private Long defaultTokenLimit;

    @Value("${demo.admin-key:}")
    private String adminKey;

    @Transactional
    public DeviceLoginResponse deviceLogin(DeviceLoginRequest request) {
        String deviceId = requireDeviceId(request.getDeviceId());
        DemoDeviceSession session = sessionRepository.findByDeviceId(deviceId)
                .orElseGet(() -> createSession(deviceId, request.getDeviceName(), request.getUserAgent()));
        session.setDeviceName(firstNonBlank(request.getDeviceName(), session.getDeviceName(), "演示设备"));
        session.setUserAgent(firstNonBlank(request.getUserAgent(), session.getUserAgent(), ""));
        session.setLastSeenAt(LocalDateTime.now());
        sessionRepository.save(session);

        DemoDeviceQuota quota = ensureQuota(deviceId, session.getDeviceName());
        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new ApiException(401, "Device owner user not found"));
        String token = jwtTokenUtil.generateDeviceToken(user.getId(), user.getUsername(), deviceId);

        DeviceLoginResponse response = new DeviceLoginResponse();
        response.setToken(token);
        response.setDeviceId(deviceId);
        response.setUserId(user.getId());
        response.setDeviceName(session.getDeviceName());
        response.setQuota(toQuotaStatus(quota));
        return response;
    }

    @Transactional
    public void touchDevice(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return;
        }
        sessionRepository.findByDeviceId(deviceId).ifPresent(session -> {
            session.setLastSeenAt(LocalDateTime.now());
            sessionRepository.save(session);
        });
    }

    @Transactional(readOnly = true)
    public QuotaStatusResponse currentQuota() {
        String deviceId = DemoDeviceContext.getDeviceId();
        if (deviceId == null || deviceId.isBlank()) {
            throw new ApiException(401, "Device token is required");
        }
        return toQuotaStatus(loadQuota(deviceId));
    }

    @Transactional(readOnly = true)
    public List<AdminDeviceQuotaResponse> listDevices(String providedAdminKey) {
        requireAdminKey(providedAdminKey);
        List<DemoDeviceSession> sessions = sessionRepository.findAll();
        return quotaRepository.findAllByOrderByUpdateTimeDesc().stream()
                .map(quota -> toAdminResponse(quota, sessions.stream()
                        .filter(session -> quota.getDeviceId().equals(session.getDeviceId()))
                        .findFirst()
                        .orElse(null), false))
                .sorted(Comparator.comparing(AdminDeviceQuotaResponse::getLastSeenAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminDeviceQuotaResponse getDevice(String deviceId, String providedAdminKey) {
        requireAdminKey(providedAdminKey);
        DemoDeviceQuota quota = loadQuota(deviceId);
        DemoDeviceSession session = sessionRepository.findByDeviceId(deviceId).orElse(null);
        return toAdminResponse(quota, session, true);
    }

    @Transactional
    public AdminDeviceQuotaResponse updateDevice(String deviceId, UpdateDeviceQuotaRequest request, String providedAdminKey) {
        requireAdminKey(providedAdminKey);
        DemoDeviceQuota quota = loadQuota(deviceId);
        if (request.getDisplayName() != null) {
            quota.setDisplayName(request.getDisplayName().trim());
        }
        if (request.getQuotaLimit() != null) {
            quota.setQuotaLimit(Math.max(0L, request.getQuotaLimit()));
        }
        if (request.getWhitelistEnabled() != null) {
            quota.setWhitelistEnabled(request.getWhitelistEnabled());
        }
        if (request.getEnabled() != null) {
            quota.setEnabled(request.getEnabled());
        }
        quota = quotaRepository.save(quota);
        return toAdminResponse(quota, sessionRepository.findByDeviceId(deviceId).orElse(null), true);
    }

    @Transactional
    public QuotaStatusResponse resetDevice(String deviceId, String providedAdminKey) {
        requireAdminKey(providedAdminKey);
        DemoDeviceQuota quota = loadQuota(deviceId);
        quota.setQuotaUsed(0L);
        quota.setPeriodStart(LocalDateTime.now());
        quota.setPeriodEnd(null);
        return toQuotaStatus(quotaRepository.save(quota));
    }

    @Transactional(readOnly = true)
    public void requireAvailable(Long userId, String operationType, Long estimatedTokens) {
        String deviceId = resolveDeviceId(userId);
        DemoDeviceQuota quota = loadQuota(deviceId);
        if (!Boolean.TRUE.equals(quota.getEnabled())) {
            throw new ApiException(403, "当前设备已被管理员禁用");
        }
        if (Boolean.TRUE.equals(quota.getWhitelistEnabled())) {
            return;
        }
        long required = Math.max(1L, estimatedTokens == null ? 1L : estimatedTokens);
        long remaining = Math.max(0L, quota.getQuotaLimit() - quota.getQuotaUsed());
        if (remaining < required) {
            throw new ApiException(429, QUOTA_EXCEEDED_MESSAGE);
        }
    }

    @Transactional
    public void recordUsage(Long userId,
                            Long skillId,
                            String sourceType,
                            String operationType,
                            Long requestTokens,
                            Long responseTokens,
                            boolean estimated,
                            String externalRunId,
                            String status) {
        String deviceId = resolveDeviceId(userId);
        Long ownerUserId = userId == null
                ? sessionRepository.findByDeviceId(deviceId)
                    .map(DemoDeviceSession::getUserId)
                    .orElseThrow(() -> new ApiException(401, "Device session not found"))
                : userId;
        DemoDeviceQuota quota = loadQuota(deviceId);
        long request = Math.max(0L, requestTokens == null ? 0L : requestTokens);
        long response = Math.max(0L, responseTokens == null ? 0L : responseTokens);

        AiTokenUsageLog log = new AiTokenUsageLog();
        log.setDeviceId(deviceId);
        log.setUserId(ownerUserId);
        log.setSkillId(skillId);
        log.setSourceType(sourceType);
        log.setOperationType(operationType);
        log.setRequestTokens(request);
        log.setResponseTokens(response);
        log.setEstimated(estimated);
        log.setExternalRunId(externalRunId);
        log.setStatus(status == null ? "SUCCESS" : status);
        usageLogRepository.save(log);

        if (!Boolean.TRUE.equals(quota.getWhitelistEnabled())) {
            quota.setQuotaUsed(Math.max(0L, quota.getQuotaUsed()) + request + response);
            quotaRepository.save(quota);
        }
    }

    public long estimateTokens(String... texts) {
        long chars = 0L;
        if (texts != null) {
            for (String text : texts) {
                if (text != null) {
                    chars += text.length();
                }
            }
        }
        return Math.max(1L, (long) Math.ceil(chars / 2.5d));
    }

    private DemoDeviceSession createSession(String deviceId, String deviceName, String userAgent) {
        User user = new User();
        user.setUsername("device_" + sha256(deviceId).substring(0, 32));
        user.setPassword("device-session-" + UUID.randomUUID());
        user.setEmail(null);
        user.setEnabled(true);
        user = userRepository.save(user);

        DemoDeviceSession session = new DemoDeviceSession();
        session.setDeviceId(deviceId);
        session.setUserId(user.getId());
        session.setDeviceName(firstNonBlank(deviceName, "演示设备"));
        session.setUserAgent(firstNonBlank(userAgent, ""));
        return sessionRepository.save(session);
    }

    private DemoDeviceQuota ensureQuota(String deviceId, String deviceName) {
        return quotaRepository.findByDeviceId(deviceId).orElseGet(() -> {
            DemoDeviceQuota quota = new DemoDeviceQuota();
            quota.setDeviceId(deviceId);
            quota.setDisplayName(firstNonBlank(deviceName, "演示设备"));
            quota.setQuotaLimit(defaultTokenLimit == null ? 100000L : defaultTokenLimit);
            quota.setQuotaUsed(0L);
            quota.setWhitelistEnabled(false);
            quota.setEnabled(true);
            quota.setPeriodStart(LocalDateTime.now());
            return quotaRepository.save(quota);
        });
    }

    private DemoDeviceQuota loadQuota(String deviceId) {
        return quotaRepository.findByDeviceId(requireDeviceId(deviceId))
                .orElseThrow(() -> new ApiException(401, "Device quota not found"));
    }

    private String resolveDeviceId(Long userId) {
        String deviceId = DemoDeviceContext.getDeviceId();
        if (deviceId != null && !deviceId.isBlank()) {
            return deviceId;
        }
        if (userId == null) {
            throw new ApiException(401, "Device token is required");
        }
        return sessionRepository.findByUserId(userId)
                .map(DemoDeviceSession::getDeviceId)
                .orElseThrow(() -> new ApiException(401, "Device session not found"));
    }

    private void requireAdminKey(String providedAdminKey) {
        if (adminKey == null || adminKey.isBlank()) {
            throw new ApiException(403, "ADMIN_CONSOLE_KEY is not configured");
        }
        if (providedAdminKey == null || !adminKey.equals(providedAdminKey)) {
            throw new ApiException(403, "Invalid admin key");
        }
    }

    private QuotaStatusResponse toQuotaStatus(DemoDeviceQuota quota) {
        QuotaStatusResponse response = new QuotaStatusResponse();
        response.setDeviceId(quota.getDeviceId());
        response.setLimit(quota.getQuotaLimit());
        response.setUsed(quota.getQuotaUsed());
        response.setWhitelisted(quota.getWhitelistEnabled());
        response.setEnabled(quota.getEnabled());
        response.setUnlimited(Boolean.TRUE.equals(quota.getWhitelistEnabled()));
        response.setRemaining(Boolean.TRUE.equals(quota.getWhitelistEnabled())
                ? Long.MAX_VALUE
                : Math.max(0L, quota.getQuotaLimit() - quota.getQuotaUsed()));
        return response;
    }

    private AdminDeviceQuotaResponse toAdminResponse(DemoDeviceQuota quota, DemoDeviceSession session, boolean includeLogs) {
        AdminDeviceQuotaResponse response = new AdminDeviceQuotaResponse();
        response.setDeviceId(quota.getDeviceId());
        response.setUserId(session == null ? null : session.getUserId());
        response.setDeviceName(session == null ? null : session.getDeviceName());
        response.setDisplayName(quota.getDisplayName());
        response.setUserAgent(session == null ? null : session.getUserAgent());
        response.setQuotaLimit(quota.getQuotaLimit());
        response.setQuotaUsed(quota.getQuotaUsed());
        response.setRemaining(Math.max(0L, quota.getQuotaLimit() - quota.getQuotaUsed()));
        response.setWhitelistEnabled(quota.getWhitelistEnabled());
        response.setEnabled(quota.getEnabled());
        response.setLastSeenAt(session == null ? null : session.getLastSeenAt());
        response.setCreateTime(session == null ? quota.getCreateTime() : session.getCreateTime());
        if (includeLogs) {
            response.setUsageLogs(usageLogRepository.findByDeviceIdOrderByCreateTimeDesc(
                    quota.getDeviceId(), PageRequest.of(0, 30)).stream().map(this::toUsageLogResponse).toList());
        }
        return response;
    }

    private TokenUsageLogResponse toUsageLogResponse(AiTokenUsageLog log) {
        TokenUsageLogResponse response = new TokenUsageLogResponse();
        response.setId(log.getId());
        response.setDeviceId(log.getDeviceId());
        response.setUserId(log.getUserId());
        response.setSkillId(log.getSkillId());
        response.setSourceType(log.getSourceType());
        response.setOperationType(log.getOperationType());
        response.setRequestTokens(log.getRequestTokens());
        response.setResponseTokens(log.getResponseTokens());
        response.setEstimated(log.getEstimated());
        response.setExternalRunId(log.getExternalRunId());
        response.setStatus(log.getStatus());
        response.setCreateTime(log.getCreateTime());
        return response;
    }

    private String requireDeviceId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new ApiException(400, "deviceId is required");
        }
        return value.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format(Locale.ROOT, "%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode()).replace("-", "0") + "00000000000000000000000000000000";
        }
    }
}
