package com.lifeos.integration.feishu.bot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lark.oapi.service.im.v1.model.MentionEvent;
import com.lifeos.integration.feishu.bot.dto.FeishuBotCommand;
import com.lifeos.integration.feishu.bot.dto.FeishuBotCommandType;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FeishuBotCommandService {

    private static final Pattern INTEGER_PATTERN = Pattern.compile("(\\d+)");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String extractText(String rawContent, MentionEvent[] mentions) {
        String text = rawContent == null ? "" : rawContent.trim();
        try {
            JsonNode root = objectMapper.readTree(text);
            String withoutAt = root.path("text_without_at_bot").asText("");
            String plain = root.path("text").asText("");
            text = !withoutAt.isBlank() ? withoutAt : plain;
            if (text.isBlank() && root.has("content")) {
                text = root.path("content").asText("");
            }
        } catch (Exception ignored) {
            // Feishu content can be plain text in tests or SDK callbacks.
        }

        if (mentions != null) {
            for (MentionEvent mention : mentions) {
                if (mention == null) {
                    continue;
                }
                if (mention.getKey() != null && !mention.getKey().isBlank()) {
                    text = text.replace(mention.getKey(), "");
                }
                if (mention.getName() != null && !mention.getName().isBlank()) {
                    text = text.replace("@" + mention.getName(), "");
                    text = text.replace(mention.getName(), "");
                }
            }
        }
        return text.replaceAll("^(@\\S+\\s*)+", "").trim();
    }

    public FeishuBotCommand parse(String text) {
        FeishuBotCommand command = new FeishuBotCommand();
        String value = text == null ? "" : text.trim();
        command.setOriginalText(value);
        if (value.isBlank()) {
            command.setType(FeishuBotCommandType.UNKNOWN);
            return command;
        }
        if (!value.startsWith("/")) {
            command.setType(FeishuBotCommandType.ASK);
            command.setQuestion(value);
            return command;
        }

        if (value.startsWith("/帮助") || value.equalsIgnoreCase("/help")) {
            command.setType(FeishuBotCommandType.HELP);
            return command;
        }
        if (value.startsWith("/同步")) {
            return parseSync(value, command);
        }
        if (value.startsWith("/蒸馏")) {
            command.setType(FeishuBotCommandType.DISTILL);
            return command;
        }
        if (value.startsWith("/资料")) {
            return parseManualDocument(value, command);
        }
        if (value.startsWith("/任务")) {
            return parseTask(value, command);
        }
        if (value.startsWith("/统计")) {
            command.setType(FeishuBotCommandType.STATS);
            return command;
        }
        if (value.startsWith("/反馈")) {
            return parseFeedback(value, command);
        }

        command.setType(FeishuBotCommandType.UNKNOWN);
        return command;
    }

    private FeishuBotCommand parseSync(String value, FeishuBotCommand command) {
        String payload = value.substring("/同步".length()).trim();
        if (payload.startsWith("文档")) {
            String documentRef = payload.substring("文档".length()).trim();
            command.setType(documentRef.isBlank() ? FeishuBotCommandType.UNKNOWN : FeishuBotCommandType.SYNC_DOCUMENT);
            command.setDocumentRef(documentRef);
            return command;
        }

        int limit = 20;
        Matcher matcher = INTEGER_PATTERN.matcher(payload);
        if (matcher.find()) {
            limit = Integer.parseInt(matcher.group(1));
        }
        command.setType(FeishuBotCommandType.SYNC_RECENT);
        command.setLimit(Math.max(1, Math.min(limit, 100)));
        return command;
    }

    private FeishuBotCommand parseManualDocument(String value, FeishuBotCommand command) {
        String payload = value.substring("/资料".length()).trim();
        String[] parts = splitPipe(payload);
        command.setTitle(parts[0].trim());
        command.setContent(parts.length > 1 ? parts[1].trim() : "");
        command.setType(command.getTitle().isBlank() || command.getContent().isBlank()
                ? FeishuBotCommandType.UNKNOWN
                : FeishuBotCommandType.MANUAL_DOCUMENT);
        return command;
    }

    private FeishuBotCommand parseTask(String value, FeishuBotCommand command) {
        String payload = value.substring("/任务".length()).trim();
        if (payload.startsWith("列表")) {
            command.setType(FeishuBotCommandType.TASK_LIST);
            return command;
        }
        if (payload.startsWith("新建")) {
            String taskPayload = payload.substring("新建".length()).trim();
            String[] parts = splitPipe(taskPayload);
            command.setTitle(parts[0].trim());
            command.setContent(parts.length > 1 ? parts[1].trim() : "");
            command.setType(command.getTitle().isBlank() ? FeishuBotCommandType.UNKNOWN : FeishuBotCommandType.TASK_CREATE);
            return command;
        }
        if (payload.startsWith("完成")) {
            Matcher matcher = INTEGER_PATTERN.matcher(payload);
            if (matcher.find()) {
                command.setTaskId(Long.parseLong(matcher.group(1)));
                command.setType(FeishuBotCommandType.TASK_COMPLETE);
            } else {
                command.setType(FeishuBotCommandType.UNKNOWN);
            }
            return command;
        }
        command.setType(FeishuBotCommandType.UNKNOWN);
        return command;
    }

    private FeishuBotCommand parseFeedback(String value, FeishuBotCommand command) {
        String payload = value.substring("/反馈".length()).trim();
        String[] parts = payload.split("\\s+", 3);
        if (parts.length < 2) {
            command.setType(FeishuBotCommandType.UNKNOWN);
            return command;
        }
        try {
            command.setQaLogId(Long.parseLong(parts[0]));
            command.setFeedbackLabel(parts[1]);
            command.setFeedbackComment(parts.length > 2 ? parts[2].trim() : "");
            command.setType(FeishuBotCommandType.FEEDBACK);
        } catch (NumberFormatException e) {
            command.setType(FeishuBotCommandType.UNKNOWN);
        }
        return command;
    }

    private String[] splitPipe(String value) {
        return value.split("\\s*[|｜]\\s*", 2);
    }
}
