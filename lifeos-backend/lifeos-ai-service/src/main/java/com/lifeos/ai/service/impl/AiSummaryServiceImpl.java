package com.lifeos.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.ai.config.AiProperties;
import com.lifeos.ai.service.AiSummaryService;
import com.lifeos.api.ai.dto.AiAsyncJobCommand;
import com.lifeos.api.ai.dto.AiNoteOrganizeResult;
import com.lifeos.api.ai.dto.AiSummaryCommand;
import com.lifeos.api.ai.dto.AiSummaryResult;
import com.lifeos.api.ai.dto.AiTaskExtractionResult;
import com.lifeos.api.ai.dto.AiTaskSuggestionDTO;
import com.lifeos.api.ai.dto.AiWeeklyReviewResultDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
/**
 * AI 服务侧的执行器。
 *
 * 这里负责真正调用大模型，生成：
 * - 摘要
 * - 整理建议
 * - 任务提取结果
 * - 周回顾内容
 *
 * 同时它还承担一个很重要的职责：
 * 当本地环境没有配置真实 API Key 时，返回稳定的 mock/fallback 结果，
 * 保证整条异步 AI 链路在开发环境下仍然可以完整跑通。
 */
public class AiSummaryServiceImpl implements AiSummaryService {

    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";

    @Resource
    private AiProperties aiProperties;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public AiSummaryResult generateSummary(AiSummaryCommand command) {
        AiSummaryResult result = new AiSummaryResult();
        result.setNoteId(command.getNoteId());
        try {
            String summary = generateSummaryText(command);
            result.setSummary(summary);
            result.setStatus(STATUS_SUCCESS);
        } catch (Exception ex) {
            log.warn("Failed to generate note summary for note {}", command.getNoteId(), ex);
            result.setStatus(STATUS_FAILED);
            result.setErrorMessage(ex.getMessage());
        }
        return result;
    }

    @Override
    public AiNoteOrganizeResult organizeNote(AiSummaryCommand command) {
        AiNoteOrganizeResult result = new AiNoteOrganizeResult();
        result.setNoteId(command.getNoteId());
        try {
            AiNoteOrganizeResult payload = buildOrganizeResult(command);
            result.setSuggestedTitle(payload.getSuggestedTitle());
            result.setSuggestedTags(payload.getSuggestedTags());
            result.setSummary(payload.getSummary());
            result.setStatus(STATUS_SUCCESS);
        } catch (Exception ex) {
            log.warn("Failed to organize note {}", command.getNoteId(), ex);
            result.setStatus(STATUS_FAILED);
            result.setErrorMessage(ex.getMessage());
        }
        return result;
    }

    @Override
    public AiTaskExtractionResult extractTasks(AiSummaryCommand command) {
        AiTaskExtractionResult result = new AiTaskExtractionResult();
        result.setNoteId(command.getNoteId());
        try {
            AiTaskExtractionResult payload = buildTaskExtractionResult(command);
            result.setTasks(payload.getTasks());
            result.setStatus(STATUS_SUCCESS);
        } catch (Exception ex) {
            log.warn("Failed to extract tasks from note {}", command.getNoteId(), ex);
            result.setStatus(STATUS_FAILED);
            result.setErrorMessage(ex.getMessage());
        }
        return result;
    }

    @Override
    public AiWeeklyReviewResultDTO generateWeeklyReview(AiAsyncJobCommand command) {
        try {
            if (!StringUtils.hasText(aiProperties.getApiKey())) {
                // 本地开发或演示环境通常没有真实模型密钥，
                // 这里直接返回可预测的 mock 结果，让前后端联调不被外部依赖卡住。
                return buildMockWeeklyReview(command);
            }

            String response = requestTextJson(buildWeeklyReviewPrompt(command));
            JsonNode root = objectMapper.readTree(stripMarkdownCodeFence(response));
            AiWeeklyReviewResultDTO result = new AiWeeklyReviewResultDTO();
            result.setHeadline(readText(root, "headline", "本周知识回顾"));
            result.setSummary(readText(root, "summary", buildMockWeeklyReview(command).getSummary()));
            result.setHighlights(readStringList(root.path("highlights"), buildMockWeeklyReview(command).getHighlights()));
            result.setFocusAreas(readStringList(root.path("focusAreas"), buildMockWeeklyReview(command).getFocusAreas()));
            result.setNextActions(readStringList(root.path("nextActions"), buildMockWeeklyReview(command).getNextActions()));
            return result;
        } catch (Exception ex) {
            log.warn("Failed to build weekly review for user {}", command.getUserId(), ex);
            return buildMockWeeklyReview(command);
        }
    }

    private String generateSummaryText(AiSummaryCommand command) throws Exception {
        if (!StringUtils.hasText(aiProperties.getApiKey())) {
            return buildMockSummary(command);
        }
        return requestTextSummary(command);
    }

    private AiNoteOrganizeResult buildOrganizeResult(AiSummaryCommand command) throws Exception {
        if (!StringUtils.hasText(aiProperties.getApiKey())) {
            return buildMockOrganizeResult(command);
        }

        String response = requestTextJson(buildOrganizePrompt(command));
        JsonNode root = objectMapper.readTree(stripMarkdownCodeFence(response));

        AiNoteOrganizeResult result = new AiNoteOrganizeResult();
        result.setNoteId(command.getNoteId());
        result.setSuggestedTitle(readText(root, "suggestedTitle", buildFallbackTitle(command)));
        result.setSuggestedTags(readText(root, "suggestedTags", buildFallbackTags(command)));
        result.setSummary(readText(root, "summary", buildMockSummary(command)));
        return result;
    }

    private AiTaskExtractionResult buildTaskExtractionResult(AiSummaryCommand command) throws Exception {
        if (!StringUtils.hasText(aiProperties.getApiKey())) {
            return buildMockTaskExtractionResult(command);
        }

        String response = requestTextJson(buildTaskExtractionPrompt(command));
        JsonNode root = objectMapper.readTree(stripMarkdownCodeFence(response));

        AiTaskExtractionResult result = new AiTaskExtractionResult();
        result.setNoteId(command.getNoteId());
        JsonNode tasksNode = root.path("tasks");
        if (tasksNode.isArray()) {
            for (JsonNode taskNode : tasksNode) {
                String title = readText(taskNode, "title", null);
                if (!StringUtils.hasText(title)) {
                    continue;
                }
                AiTaskSuggestionDTO suggestion = new AiTaskSuggestionDTO();
                suggestion.setTitle(title.trim());
                suggestion.setDescription(readText(taskNode, "description", ""));
                suggestion.setTags(readText(taskNode, "tags", buildFallbackTags(command)));
                result.getTasks().add(suggestion);
                if (result.getTasks().size() >= 5) {
                    break;
                }
            }
        }
        if (result.getTasks().isEmpty()) {
            return buildMockTaskExtractionResult(command);
        }
        return result;
    }

    private String requestTextSummary(AiSummaryCommand command) {
        return requestText(buildChatRequest(
                List.of(
                        Map.of("role", "system", "content", "You summarize user notes into concise Chinese bullet-style prose."),
                        Map.of("role", "user", "content", buildSummaryPrompt(command)))));
    }

    private String requestTextJson(String prompt) {
        return requestText(buildChatRequest(
                List.of(
                        Map.of("role", "system", "content", "You are a structured JSON generator. Return valid JSON only."),
                        Map.of("role", "user", "content", prompt))));
    }

    private String requestText(Map<String, Object> requestBody) {
        // 每次按当前配置动态创建客户端，保证服务本身保持无状态，
        // 切换模型地址或密钥时不需要维护额外连接状态。
        RestClient client = RestClient.builder()
                .baseUrl(aiProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        String response = client.post()
                .uri("/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (!contentNode.isTextual() || !StringUtils.hasText(contentNode.asText())) {
                throw new RuntimeException("AI response did not contain content");
            }
            return contentNode.asText().trim();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to parse AI response", ex);
        }
    }

    private Map<String, Object> buildChatRequest(List<Map<String, String>> messages) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", aiProperties.getModel());
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.3);
        return requestBody;
    }

    private String buildSummaryPrompt(AiSummaryCommand command) {
        String title = StringUtils.hasText(command.getTitle()) ? command.getTitle() : "Untitled";
        String content = StringUtils.hasText(command.getContent()) ? command.getContent() : "";
        return "Please summarize the following note in concise Chinese.\n"
                + "Title: " + title + "\n"
                + "Content:\n" + content + "\n"
                + "Requirements: keep it short, clear, and useful.";
    }

    private String buildOrganizePrompt(AiSummaryCommand command) {
        return "Read the note and return valid JSON only with keys suggestedTitle, suggestedTags, summary.\n"
                + "suggestedTags must be a comma-separated string with up to 3 tags.\n"
                + "summary should be concise Chinese text.\n"
                + "Title: " + safeText(command.getTitle()) + "\n"
                + "Content:\n" + safeText(command.getContent());
    }

    private String buildTaskExtractionPrompt(AiSummaryCommand command) {
        return "Read the note and return valid JSON only in this shape: "
                + "{\"tasks\":[{\"title\":\"\",\"description\":\"\",\"tags\":\"\"}]}.\n"
                + "Return 1 to 5 actionable tasks. tags must be a comma-separated string.\n"
                + "Title: " + safeText(command.getTitle()) + "\n"
                + "Content:\n" + safeText(command.getContent());
    }

    private String buildWeeklyReviewPrompt(AiAsyncJobCommand command) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Return valid JSON only with keys headline, summary, highlights, focusAreas, nextActions.\n")
                .append("Each array should contain 2 to 4 concise Chinese strings.\n")
                .append("Summarize the user's past week as a personal knowledge review, using the data below.\n");

        if (command.getDashboard() != null) {
            prompt.append("Notes to review: ").append(defaultNumber(command.getDashboard().getNotesToReviewCount())).append('\n')
                    .append("Week new notes: ").append(defaultNumber(command.getDashboard().getWeekNewNoteCount())).append('\n')
                    .append("Week organized notes: ").append(defaultNumber(command.getDashboard().getWeekOrganizedNoteCount())).append('\n')
                    .append("Pending tasks: ").append(defaultNumber(command.getDashboard().getPendingTaskCount())).append('\n')
                    .append("Pending extracted tasks: ").append(defaultNumber(command.getDashboard().getPendingExtractedTaskCount())).append('\n')
                    .append("AI inbox: ").append(defaultNumber(command.getDashboard().getAiInboxCount())).append('\n');

            if (command.getDashboard().getTopTags() != null && !command.getDashboard().getTopTags().isEmpty()) {
                prompt.append("Top tags:\n");
                command.getDashboard().getTopTags().forEach(tag ->
                        prompt.append("- ").append(tag.getTag()).append(" (").append(defaultNumber(tag.getCount())).append(")\n"));
            }

            if (command.getDashboard().getRecentNotes() != null && !command.getDashboard().getRecentNotes().isEmpty()) {
                prompt.append("Recent notes:\n");
                command.getDashboard().getRecentNotes().forEach(note ->
                        prompt.append("- ").append(safeText(note.getTitle()))
                                .append(" | tags: ").append(safeText(note.getTags()))
                                .append(" | state: ").append(safeText(note.getReviewState()))
                                .append(" | updated: ").append(safeText(note.getUpdatedAt()))
                                .append('\n'));
            }
        }

        return prompt.toString();
    }

    private AiNoteOrganizeResult buildMockOrganizeResult(AiSummaryCommand command) {
        AiNoteOrganizeResult result = new AiNoteOrganizeResult();
        result.setNoteId(command.getNoteId());
        result.setSuggestedTitle(buildFallbackTitle(command));
        result.setSuggestedTags(buildFallbackTags(command));
        result.setSummary(buildMockSummary(command));
        return result;
    }

    private AiTaskExtractionResult buildMockTaskExtractionResult(AiSummaryCommand command) {
        // fallback 提取逻辑会尽量把笔记中的行动句转换成任务，
        // 这样即使没接真实模型，任务提取页面也能表现出完整效果。
        AiTaskExtractionResult result = new AiTaskExtractionResult();
        result.setNoteId(command.getNoteId());

        List<String> candidates = extractActionCandidates(command.getContent());
        if (candidates.isEmpty()) {
            candidates.add("Review note: " + buildFallbackTitle(command));
        }

        for (String candidate : candidates) {
            AiTaskSuggestionDTO suggestion = new AiTaskSuggestionDTO();
            suggestion.setTitle(truncate(candidate, 60));
            suggestion.setDescription("Created from note \"" + buildFallbackTitle(command) + "\"");
            suggestion.setTags(buildFallbackTags(command));
            result.getTasks().add(suggestion);
            if (result.getTasks().size() >= 5) {
                break;
            }
        }
        return result;
    }

    private AiWeeklyReviewResultDTO buildMockWeeklyReview(AiAsyncJobCommand command) {
        AiWeeklyReviewResultDTO result = new AiWeeklyReviewResultDTO();
        result.setHeadline("本周知识回顾");

        long reviewCount = 0L;
        long newNoteCount = 0L;
        long organizedCount = 0L;
        long pendingTasks = 0L;
        if (command.getDashboard() != null) {
            reviewCount = defaultNumber(command.getDashboard().getNotesToReviewCount());
            newNoteCount = defaultNumber(command.getDashboard().getWeekNewNoteCount());
            organizedCount = defaultNumber(command.getDashboard().getWeekOrganizedNoteCount());
            pendingTasks = defaultNumber(command.getDashboard().getPendingExtractedTaskCount());
        }

        result.setSummary(String.format(Locale.ROOT,
                "本周新增 %d 篇笔记，整理 %d 篇，当前有 %d 篇待复习笔记和 %d 项待处理的知识任务。",
                newNoteCount, organizedCount, reviewCount, pendingTasks));
        result.getHighlights().add(String.format(Locale.ROOT, "本周累计沉淀 %d 篇新笔记。", newNoteCount));
        result.getHighlights().add(String.format(Locale.ROOT, "已有 %d 篇笔记完成 AI 整理。", organizedCount));
        result.getHighlights().add(String.format(Locale.ROOT, "当前复习队列中有 %d 篇重点笔记。", reviewCount));

        if (command.getDashboard() != null && command.getDashboard().getTopTags() != null) {
            command.getDashboard().getTopTags().stream()
                    .limit(3)
                    .forEach(tag -> result.getFocusAreas().add("持续关注主题：" + tag.getTag()));
        }
        if (result.getFocusAreas().isEmpty()) {
            result.getFocusAreas().add("补充更多标签，让知识主题更清晰。");
            result.getFocusAreas().add("从近期笔记中挑选值得长期维护的内容。");
        }

        result.getNextActions().add("优先处理待复习笔记，补上摘要和标签。");
        if (pendingTasks > 0) {
            result.getNextActions().add("把待处理的知识任务拆成更小的下一步。");
        }
        result.getNextActions().add("挑选一篇高价值笔记，继续整理成常青内容。");
        return result;
    }

    private List<String> extractActionCandidates(String content) {
        Set<String> suggestions = new LinkedHashSet<>();
        if (StringUtils.hasText(content)) {
            for (String line : content.split("\\r?\\n")) {
                String normalized = normalizeActionLine(line);
                if (StringUtils.hasText(normalized)) {
                    suggestions.add(normalized);
                }
                if (suggestions.size() >= 5) {
                    break;
                }
            }

            if (suggestions.isEmpty()) {
                for (String sentence : content.split("[。.!?；;]")) {
                    String normalized = normalizeActionLine(sentence);
                    if (StringUtils.hasText(normalized)) {
                        suggestions.add(normalized);
                    }
                    if (suggestions.size() >= 5) {
                        break;
                    }
                }
            }
        }
        return new ArrayList<>(suggestions);
    }

    private String normalizeActionLine(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String normalized = raw.trim()
                .replaceFirst("^[-*+\\d.\\s]+", "")
                .replaceAll("\\s+", " ");
        if (normalized.length() < 6) {
            return null;
        }
        return normalized;
    }

    private String buildMockSummary(AiSummaryCommand command) {
        String title = StringUtils.hasText(command.getTitle()) ? command.getTitle().trim() : "Untitled";
        String content = StringUtils.hasText(command.getContent()) ? command.getContent().trim() : "";
        String normalized = content.replaceAll("\\s+", " ");
        if (normalized.length() > 120) {
            normalized = normalized.substring(0, 120) + "...";
        }
        if (!StringUtils.hasText(normalized)) {
            normalized = "This note is currently empty and is waiting for more detailed content.";
        }
        return String.format(Locale.ROOT, "Summary of \"%s\": %s", title, normalized);
    }

    private String buildFallbackTitle(AiSummaryCommand command) {
        if (StringUtils.hasText(command.getTitle())) {
            return command.getTitle().trim();
        }
        String content = safeText(command.getContent()).replaceAll("\\s+", " ").trim();
        if (!StringUtils.hasText(content)) {
            return "Untitled Note";
        }
        return truncate(content, 32);
    }

    private String buildFallbackTags(AiSummaryCommand command) {
        if (StringUtils.hasText(command.getContent()) || StringUtils.hasText(command.getTitle())) {
            String source = (safeText(command.getTitle()) + " " + safeText(command.getContent()))
                    .replaceAll("[^\\p{L}\\p{N}\\s]", " ");
            Set<String> tags = new LinkedHashSet<>();
            for (String token : source.split("\\s+")) {
                String normalized = token.trim();
                if (normalized.length() < 3) {
                    continue;
                }
                tags.add(normalized);
                if (tags.size() >= 3) {
                    break;
                }
            }
            if (!tags.isEmpty()) {
                return String.join(", ", tags);
            }
        }
        return "knowledge, note";
    }

    private String readText(JsonNode node, String fieldName, String fallback) {
        JsonNode value = node.path(fieldName);
        if (value.isTextual() && StringUtils.hasText(value.asText())) {
            return value.asText().trim();
        }
        return fallback;
    }

    private List<String> readStringList(JsonNode node, List<String> fallback) {
        List<String> values = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode item : node) {
                if (item.isTextual() && StringUtils.hasText(item.asText())) {
                    values.add(item.asText().trim());
                }
            }
        }
        return values.isEmpty() ? fallback : values;
    }

    private long defaultNumber(Long value) {
        return value == null ? 0L : value;
    }

    private String stripMarkdownCodeFence(String text) {
        String normalized = safeText(text).trim();
        if (!normalized.startsWith("```")) {
            return normalized;
        }

        int firstNewLine = normalized.indexOf('\n');
        int lastFence = normalized.lastIndexOf("```");
        if (firstNewLine < 0 || lastFence <= firstNewLine) {
            return normalized.replace("```", "").trim();
        }
        return normalized.substring(firstNewLine + 1, lastFence).trim();
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength).trim();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}
