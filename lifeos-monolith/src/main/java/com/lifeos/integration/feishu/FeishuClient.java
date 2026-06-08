package com.lifeos.integration.feishu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Feishu OpenAPI adapter. It only uses real tenant credentials and lets callers
 * surface failures instead of fabricating handoff sources.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeishuClient {

    private final FeishuProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String cachedTenantToken;
    private Instant tokenExpiresAt = Instant.EPOCH;

    public boolean isConfigured() {
        return properties.hasCredentials();
    }

    public FeishuSourceItem fetchDocument(String documentRef) {
        if (!properties.hasCredentials()) {
            throw new IllegalStateException("Feishu credentials are not configured");
        }

        String documentId = normalizeDocumentRef(documentRef);
        String url = apiUrl("/docx/v1/documents/" + documentId + "/raw_content");

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    String.class
            );
            JsonNode root = objectMapper.readTree(response.getBody());
            ensureFeishuSuccess(root);
            JsonNode data = root.path("data");
            String title = firstText(data, "title", "document_title", "name");
            String content = firstText(data, "content", "raw_content", "text");
            if (content.isBlank()) {
                content = data.toString();
            }
            return new FeishuSourceItem(
                    "FEISHU_DOC",
                    documentId,
                    title.isBlank() ? "飞书文档 " + documentId : title,
                    content,
                    LocalDateTime.now()
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch Feishu document " + documentId + ": " + e.getMessage(), e);
        }
    }

    public List<FeishuSourceItem> fetchChatMessages(String chatId,
                                                    LocalDateTime startTime,
                                                    LocalDateTime endTime,
                                                    int limit) {
        if (!properties.hasCredentials()) {
            throw new IllegalStateException("Feishu credentials are not configured");
        }
        if (chatId == null || chatId.isBlank()) {
            return List.of();
        }

        List<FeishuSourceItem> items = new ArrayList<>();
        String pageToken = null;
        int pageSize = Math.min(Math.max(limit, 1), 50);

        try {
            do {
                UriComponentsBuilder builder = UriComponentsBuilder
                        .fromHttpUrl(apiUrl("/im/v1/messages"))
                        .queryParam("container_id_type", "chat")
                        .queryParam("container_id", chatId)
                        .queryParam("sort_type", "ByCreateTimeAsc")
                        .queryParam("page_size", pageSize);

                if (pageToken != null && !pageToken.isBlank()) {
                    builder.queryParam("page_token", pageToken);
                }
                if (startTime != null) {
                    builder.queryParam("start_time", toEpochSecond(startTime));
                }
                if (endTime != null) {
                    builder.queryParam("end_time", toEpochSecond(endTime));
                }

                ResponseEntity<String> response = restTemplate.exchange(
                        builder.toUriString(),
                        HttpMethod.GET,
                        new HttpEntity<>(headers()),
                        String.class
                );
                JsonNode root = objectMapper.readTree(response.getBody());
                ensureFeishuSuccess(root);
                JsonNode data = root.path("data");
                JsonNode messageItems = data.path("items");
                if (messageItems.isArray()) {
                    for (JsonNode message : messageItems) {
                        FeishuSourceItem item = toSourceItem(chatId, message);
                        if (item != null) {
                            items.add(item);
                        }
                        if (items.size() >= limit) {
                            return items;
                        }
                    }
                }
                pageToken = data.path("page_token").asText(null);
                if (!data.path("has_more").asBoolean(false)) {
                    pageToken = null;
                }
            } while (pageToken != null && items.size() < limit);

            return items;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch Feishu chat messages: " + e.getMessage(), e);
        }
    }

    public void replyText(String messageId, String text) {
        if (!properties.hasCredentials()) {
            throw new IllegalStateException("Feishu credentials are not configured");
        }
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("Feishu message_id is required");
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("msg_type", "text");
            body.put("content", objectMapper.writeValueAsString(Map.of("text", text == null ? "" : text)));
            body.put("reply_in_thread", true);
            body.put("uuid", UUID.randomUUID().toString());
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl("/im/v1/messages/" + messageId + "/reply"),
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers()),
                    String.class
            );
            JsonNode root = objectMapper.readTree(response.getBody());
            ensureFeishuSuccess(root);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to reply Feishu message " + messageId + ": " + e.getMessage(), e);
        }
    }

    private FeishuSourceItem toSourceItem(String chatId, JsonNode message) {
        String messageId = message.path("message_id").asText(UUID.randomUUID().toString());
        String sender = message.path("sender").path("sender_id").path("user_id").asText("unknown");
        String messageType = message.path("msg_type").asText("text");
        String createTime = message.path("create_time").asText("");
        String content = extractMessageContent(message.path("body").path("content").asText(""));
        if (!isKnowledgeMessage(messageType, content)) {
            return null;
        }
        LocalDateTime sourceTime = parseMessageTime(createTime);

        return new FeishuSourceItem(
                "FEISHU_CHAT",
                chatId + ":" + messageId,
                "群聊消息 - " + sender + " / " + messageType,
                content,
                sourceTime
        );
    }

    private boolean isKnowledgeMessage(String messageType, String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        String type = messageType == null ? "" : messageType.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("text", "post").contains(type)) {
            return false;
        }
        String trimmed = content.trim();
        return !(trimmed.startsWith("/")
                || trimmed.startsWith("@")
                || trimmed.startsWith("{\"template\"")
                || trimmed.startsWith("{\"text_without_at_bot\"")
                || isBotOperationalMessage(trimmed));
    }

    private boolean isBotOperationalMessage(String content) {
        return content.startsWith("已收到，正在处理")
                || content.startsWith("处理失败：")
                || content.startsWith("可用指令：")
                || content.startsWith("同步完成：")
                || content.startsWith("飞书文档同步完成：")
                || content.startsWith("蒸馏完成：")
                || content.startsWith("资料已入库：")
                || content.startsWith("行动项已")
                || content.startsWith("当前 Skill 近 30 天统计")
                || content.contains("QA Log ID:");
    }

    private String extractMessageContent(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(rawContent);
            String text = firstText(root, "text", "content");
            if (!text.isBlank()) {
                return text;
            }
        } catch (Exception ignored) {
            // Raw content is sometimes already plain text.
        }
        return rawContent;
    }

    private LocalDateTime parseMessageTime(String createTime) {
        try {
            long epochMillis = Long.parseLong(createTime);
            if (epochMillis < 10_000_000_000L) {
                epochMillis *= 1000;
            }
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getTenantAccessToken());
        return headers;
    }

    private synchronized String getTenantAccessToken() {
        if (cachedTenantToken != null && Instant.now().isBefore(tokenExpiresAt.minusSeconds(60))) {
            return cachedTenantToken;
        }

        try {
            Map<String, String> body = Map.of(
                    "app_id", properties.getAppId(),
                    "app_secret", properties.getAppSecret()
            );
            ResponseEntity<String> response = restTemplate.postForEntity(
                    apiUrl("/auth/v3/tenant_access_token/internal"),
                    new HttpEntity<>(body, jsonHeaders()),
                    String.class
            );
            JsonNode root = objectMapper.readTree(response.getBody());
            ensureFeishuSuccess(root);
            cachedTenantToken = root.path("tenant_access_token").asText(root.path("data").path("tenant_access_token").asText());
            int expire = root.path("expire").asInt(root.path("data").path("expire").asInt(7200));
            tokenExpiresAt = Instant.now().plusSeconds(Math.max(expire, 600));
            return cachedTenantToken;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get Feishu tenant access token: " + e.getMessage(), e);
        }
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private void ensureFeishuSuccess(JsonNode root) {
        int code = root.path("code").asInt(0);
        if (code != 0) {
            throw new IllegalStateException(root.path("msg").asText("Feishu API returned code " + code));
        }
    }

    private String normalizeDocumentRef(String documentRef) {
        String value = documentRef == null ? "" : documentRef.trim();
        int queryIndex = value.indexOf('?');
        if (queryIndex >= 0) {
            value = value.substring(0, queryIndex);
        }
        int hashIndex = value.indexOf('#');
        if (hashIndex >= 0) {
            value = value.substring(0, hashIndex);
        }
        String[] separators = {"/docx/", "/wiki/", "/docs/", "/doc/"};
        for (String separator : separators) {
            int index = value.indexOf(separator);
            if (index >= 0) {
                value = value.substring(index + separator.length());
                break;
            }
        }
        int slashIndex = value.lastIndexOf('/');
        if (slashIndex >= 0) {
            value = value.substring(slashIndex + 1);
        }
        return value;
    }

    private String firstText(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.path(name);
            if (value.isTextual() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return "";
    }

    private long toEpochSecond(LocalDateTime value) {
        return value.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    private String apiUrl(String path) {
        return properties.getBaseUrl().replaceAll("/$", "") + path;
    }
}
