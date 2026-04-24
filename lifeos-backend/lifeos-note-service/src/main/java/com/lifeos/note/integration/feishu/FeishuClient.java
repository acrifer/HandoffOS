package com.lifeos.note.integration.feishu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.note.config.FeishuProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class FeishuClient {

    private static final Pattern DOCUMENT_PATH_PATTERN = Pattern.compile("/(?:docx|docs)/([A-Za-z0-9_\\-]+)");
    private static final int DEFAULT_CHAT_LIMIT = 80;
    private static final int MAX_CHAT_LIMIT = 200;

    @Resource
    private FeishuProperties feishuProperties;

    @Resource
    private ObjectMapper objectMapper;

    private volatile String tenantAccessToken;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    public FeishuSourcePayload fetchDocument(String documentRef) {
        ensureConfigured();
        String documentId = normalizeDocumentId(documentRef);
        String body = getWithAuth("/docx/v1/documents/" + documentId + "/raw_content", null);
        try {
            JsonNode root = objectMapper.readTree(body);
            ensureFeishuSuccess(root);
            String content = root.path("data").path("content").asText("");
            FeishuSourcePayload payload = new FeishuSourcePayload();
            payload.setSourceType("FEISHU_DOC");
            payload.setExternalId(documentId);
            payload.setTitle("飞书文档 " + documentId);
            payload.setContent(content);
            payload.setSourceTime(new Date());
            return payload;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to parse Feishu document response", ex);
        }
    }

    public List<FeishuSourcePayload> fetchChatMessages(String chatId, Date startTime, Date endTime, Integer limit) {
        ensureConfigured();
        if (!StringUtils.hasText(chatId)) {
            return List.of();
        }

        int maxItems = Math.min(Math.max(limit == null ? DEFAULT_CHAT_LIMIT : limit, 1), MAX_CHAT_LIMIT);
        List<FeishuSourcePayload> payloads = new ArrayList<>();
        String pageToken = null;
        do {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("container_id_type", "chat");
            params.put("container_id", chatId.trim());
            params.put("page_size", String.valueOf(Math.min(50, maxItems - payloads.size())));
            if (startTime != null) {
                params.put("start_time", String.valueOf(startTime.getTime() / 1000));
            }
            if (endTime != null) {
                params.put("end_time", String.valueOf(endTime.getTime() / 1000));
            }
            if (StringUtils.hasText(pageToken)) {
                params.put("page_token", pageToken);
            }

            String body = getWithAuth("/im/v1/messages", params);
            try {
                JsonNode root = objectMapper.readTree(body);
                ensureFeishuSuccess(root);
                JsonNode data = root.path("data");
                for (JsonNode item : data.path("items")) {
                    FeishuSourcePayload payload = toMessagePayload(chatId, item);
                    if (StringUtils.hasText(payload.getContent())) {
                        payloads.add(payload);
                    }
                    if (payloads.size() >= maxItems) {
                        break;
                    }
                }
                pageToken = data.path("page_token").asText(null);
                if (!data.path("has_more").asBoolean(false)) {
                    pageToken = null;
                }
            } catch (Exception ex) {
                throw new RuntimeException("Failed to parse Feishu message response", ex);
            }
        } while (StringUtils.hasText(pageToken) && payloads.size() < maxItems);

        return payloads;
    }

    public String normalizeDocumentId(String documentRef) {
        if (!StringUtils.hasText(documentRef)) {
            throw new RuntimeException("Feishu document reference is required");
        }
        String trimmed = documentRef.trim();
        Matcher matcher = DOCUMENT_PATH_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1);
        }
        int queryStart = trimmed.indexOf('?');
        if (queryStart >= 0) {
            trimmed = trimmed.substring(0, queryStart);
        }
        int hashStart = trimmed.indexOf('#');
        if (hashStart >= 0) {
            trimmed = trimmed.substring(0, hashStart);
        }
        int slash = trimmed.lastIndexOf('/');
        if (slash >= 0) {
            trimmed = trimmed.substring(slash + 1);
        }
        if (!trimmed.matches("[A-Za-z0-9_\\-]+")) {
            throw new RuntimeException("Invalid Feishu document reference");
        }
        return trimmed;
    }

    public String contentHash(String sourceType, String externalId, String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String raw = safe(sourceType) + "|" + safe(externalId) + "|" + safe(content);
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to hash Feishu content", ex);
        }
    }

    private FeishuSourcePayload toMessagePayload(String chatId, JsonNode item) {
        String messageId = item.path("message_id").asText("");
        String content = extractMessageContent(item.path("body").path("content").asText(""));
        FeishuSourcePayload payload = new FeishuSourcePayload();
        payload.setSourceType("FEISHU_CHAT");
        payload.setExternalId(chatId + ":" + messageId);
        payload.setTitle("飞书群聊 " + chatId);
        payload.setContent(content);
        long createTime = item.path("create_time").asLong(0L);
        if (createTime > 0) {
            payload.setSourceTime(new Date(createTime));
        }
        return payload;
    }

    private String extractMessageContent(String rawContent) {
        if (!StringUtils.hasText(rawContent)) {
            return "";
        }
        try {
            JsonNode content = objectMapper.readTree(rawContent);
            if (content.has("text")) {
                return content.path("text").asText("");
            }
            return content.toString();
        } catch (Exception ex) {
            return rawContent;
        }
    }

    private String getWithAuth(String path, Map<String, String> params) {
        return requestWithRetry("GET", path, params, null, true);
    }

    private String requestWithRetry(String method, String path, Map<String, String> params, Object body, boolean auth) {
        RuntimeException last = null;
        boolean refreshed = false;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                String uri = buildUri(path, params);
                RestClient.RequestBodySpec spec = RestClient.builder()
                        .baseUrl(normalizedBaseUrl())
                        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .build()
                        .method(org.springframework.http.HttpMethod.valueOf(method))
                        .uri(uri);
                if (auth) {
                    spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + getTenantAccessToken(false));
                }
                if (body != null) {
                    return spec.body(body).retrieve().body(String.class);
                }
                return spec.retrieve().body(String.class);
            } catch (RestClientResponseException ex) {
                HttpStatusCode status = ex.getStatusCode();
                if ((status.value() == 401 || tokenExpired(ex.getResponseBodyAsString())) && auth && !refreshed) {
                    refreshed = true;
                    getTenantAccessToken(true);
                    continue;
                }
                if ((status.value() == 429 || status.is5xxServerError()) && attempt < 2) {
                    sleepBackoff(attempt);
                    continue;
                }
                last = new RuntimeException("Feishu API request failed: " + status.value(), ex);
            } catch (RuntimeException ex) {
                last = ex;
                if (attempt < 2) {
                    sleepBackoff(attempt);
                    continue;
                }
            }
        }
        throw last == null ? new RuntimeException("Feishu API request failed") : last;
    }

    private synchronized String getTenantAccessToken(boolean forceRefresh) {
        ensureConfigured();
        if (!forceRefresh && StringUtils.hasText(tenantAccessToken) && Instant.now().isBefore(tokenExpiresAt.minusSeconds(60))) {
            return tenantAccessToken;
        }

        Map<String, String> request = Map.of(
                "app_id", feishuProperties.getAppId(),
                "app_secret", feishuProperties.getAppSecret());
        String body = requestWithRetry("POST", "/auth/v3/tenant_access_token/internal", null, request, false);
        try {
            JsonNode root = objectMapper.readTree(body);
            ensureFeishuSuccess(root);
            tenantAccessToken = root.path("tenant_access_token").asText();
            long expire = root.path("expire").asLong(7200L);
            tokenExpiresAt = Instant.now().plusSeconds(Math.max(expire, 300L));
            return tenantAccessToken;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to parse Feishu tenant token response", ex);
        }
    }

    private void ensureFeishuSuccess(JsonNode root) {
        if (root.path("code").asInt(-1) != 0) {
            throw new RuntimeException("Feishu API returned error: " + root.path("msg").asText("unknown"));
        }
    }

    private boolean tokenExpired(String body) {
        return body != null && (body.contains("99991663") || body.contains("99991661"));
    }

    private String buildUri(String path, Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(path);
        if (params != null) {
            params.forEach((key, value) -> {
                if (StringUtils.hasText(value)) {
                    builder.queryParam(key, value);
                }
            });
        }
        return builder.build(false).toUriString();
    }

    private String normalizedBaseUrl() {
        String baseUrl = feishuProperties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            return "https://open.feishu.cn/open-apis";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private void ensureConfigured() {
        if (!StringUtils.hasText(feishuProperties.getAppId()) || !StringUtils.hasText(feishuProperties.getAppSecret())) {
            throw new RuntimeException("Feishu app credentials are not configured");
        }
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep((attempt + 1L) * 200L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
