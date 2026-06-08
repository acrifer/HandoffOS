package com.lifeos.ai.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Conflict Detection Service
 * Detects conflicts and inconsistencies in knowledge sources
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConflictDetectionService {

    private final AiProperties aiProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Detect conflicts between multiple sources
     */
    public List<ConflictResult> detectConflicts(List<SourceContent> sources) {
        if (sources.size() < 2) {
            return new ArrayList<>();
        }

        if (!aiProperties.hasApiKey()) {
            throw new IllegalStateException("AI API key is not configured");
        }

        try {
            String prompt = buildConflictDetectionPrompt(sources);
            String response = callLLM(prompt);
            return parseConflicts(response);

        } catch (Exception e) {
            log.error("Failed to detect conflicts via LLM: {}", e.getMessage());
            throw new IllegalStateException("Failed to detect conflicts via LLM: " + e.getMessage(), e);
        }
    }

    /**
     * Build prompt for conflict detection
     */
    private String buildConflictDetectionPrompt(List<SourceContent> sources) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("分析以下来源之间的冲突和不一致。\n\n");

        for (int i = 0; i < sources.size(); i++) {
            SourceContent source = sources.get(i);
            prompt.append(String.format("【来源 %d - %s】\n%s\n\n",
                    i + 1,
                    source.getSourceType(),
                    source.getContent().length() > 1000 ?
                            source.getContent().substring(0, 1000) + "..." :
                            source.getContent()
            ));
        }

        prompt.append("请识别以下类型的冲突：\n");
        prompt.append("1. CONTRADICTION: 明确矛盾（如流程步骤不同）\n");
        prompt.append("2. INCONSISTENCY: 不一致（如责任人不同）\n");
        prompt.append("3. AMBIGUITY: 模糊不清（如描述含糊）\n\n");

        prompt.append("返回 JSON 格式：\n");
        prompt.append("{\n");
        prompt.append("  \"conflicts\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"type\": \"CONTRADICTION\",\n");
        prompt.append("      \"description\": \"文档说流程有3步，群聊说有4步\",\n");
        prompt.append("      \"sources\": [\"来源 1\", \"来源 2\"],\n");
        prompt.append("      \"severity\": 0.8,\n");
        prompt.append("      \"recommendation\": \"建议以最新文档为准\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}");

        return prompt.toString();
    }

    /**
     * Call LLM for conflict detection
     */
    private String callLLM(String prompt) throws Exception {
        String url = aiProperties.getBaseUrl() + "/chat/completions";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", aiProperties.getModel());
        requestBody.put("messages", Arrays.asList(
                Map.of("role", "system", "content", "你是一个知识冲突检测专家，擅长发现文档和对话中的矛盾和不一致。"),
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", 0.1);
        requestBody.put("max_tokens", 2000);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiKey());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        String response = restTemplate.postForObject(url, request, String.class);

        JsonNode root = objectMapper.readTree(response);
        return root.path("choices").get(0).path("message").path("content").asText();
    }

    /**
     * Parse conflicts from LLM response
     */
    private List<ConflictResult> parseConflicts(String response) throws Exception {
        List<ConflictResult> conflicts = new ArrayList<>();

        // Extract JSON from response
        String jsonStr = response;
        if (jsonStr.contains("```json")) {
            jsonStr = jsonStr.substring(jsonStr.indexOf("```json") + 7);
            jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
        } else if (jsonStr.contains("```")) {
            jsonStr = jsonStr.substring(jsonStr.indexOf("```") + 3);
            jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
        }

        JsonNode root = objectMapper.readTree(jsonStr.trim());
        JsonNode conflictsNode = root.path("conflicts");

        if (conflictsNode.isArray()) {
            for (JsonNode conflictNode : conflictsNode) {
                ConflictResult conflict = new ConflictResult();
                conflict.setConflictType(conflictNode.path("type").asText());
                conflict.setDescription(conflictNode.path("description").asText());

                List<String> sources = new ArrayList<>();
                JsonNode sourcesNode = conflictNode.path("sources");
                if (sourcesNode.isArray()) {
                    for (JsonNode sourceNode : sourcesNode) {
                        sources.add(sourceNode.asText());
                    }
                }
                conflict.setSources(sources);

                conflict.setSeverity(conflictNode.path("severity").asDouble(0.5));
                conflict.setRecommendation(conflictNode.path("recommendation").asText(""));

                conflicts.add(conflict);
            }
        }

        return conflicts;
    }

    /**
     * Source Content DTO
     */
    public static class SourceContent {
        private String sourceType;  // DOCUMENT, CHAT
        private String content;
        private String sourceId;

        public SourceContent() {
        }

        public SourceContent(String sourceType, String content, String sourceId) {
            this.sourceType = sourceType;
            this.content = content;
            this.sourceId = sourceId;
        }

        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getSourceId() {
            return sourceId;
        }

        public void setSourceId(String sourceId) {
            this.sourceId = sourceId;
        }
    }
}
