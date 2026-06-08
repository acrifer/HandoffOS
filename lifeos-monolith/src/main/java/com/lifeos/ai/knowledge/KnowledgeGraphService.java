package com.lifeos.ai.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.ai.knowledge.entity.KnowledgeEntity;
import com.lifeos.ai.knowledge.entity.KnowledgeRelation;
import com.lifeos.ai.knowledge.repository.KnowledgeEntityRepository;
import com.lifeos.ai.knowledge.repository.KnowledgeRelationRepository;
import com.lifeos.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Knowledge Graph Service
 * Extracts entities and relations from text to build knowledge graph
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeGraphService {

    private final KnowledgeEntityRepository entityRepository;
    private final KnowledgeRelationRepository relationRepository;
    private final AiProperties aiProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Build knowledge graph from skill sources
     */
    @Transactional
    public void buildKnowledgeGraph(Long skillId, List<String> sourceTexts) {
        log.info("Building knowledge graph for skill {}", skillId);

        // Clear existing graph
        entityRepository.deleteBySkillId(skillId);
        relationRepository.deleteBySkillId(skillId);

        // Extract entities and relations from each source
        for (String text : sourceTexts) {
            extractAndSave(skillId, text);
        }

        log.info("Knowledge graph built for skill {}: {} entities, {} relations",
                skillId,
                entityRepository.countBySkillId(skillId),
                relationRepository.countBySkillId(skillId));
    }

    /**
     * Extract entities and relations from text
     */
    private void extractAndSave(Long skillId, String text) {
        if (!aiProperties.hasApiKey()) {
            throw new IllegalStateException("AI API key is not configured");
        }

        try {
            String prompt = buildExtractionPrompt(text);
            String response = callLLM(prompt);
            parseAndSaveEntitiesAndRelations(skillId, response);

        } catch (Exception e) {
            log.error("Failed to extract via LLM: {}", e.getMessage());
            throw new IllegalStateException("Failed to extract knowledge graph via LLM: " + e.getMessage(), e);
        }
    }

    /**
     * Build prompt for entity and relation extraction
     */
    private String buildExtractionPrompt(String text) {
        return String.format(
                "从以下文本中提取实体和关系。\n\n" +
                "实体类型：\n" +
                "- PERSON: 人名\n" +
                "- PROJECT: 项目名\n" +
                "- PROCESS: 流程或步骤\n" +
                "- CONCEPT: 概念或技术\n\n" +
                "关系类型：\n" +
                "- RESPONSIBLE_FOR: 负责\n" +
                "- DEPENDS_ON: 依赖\n" +
                "- PREREQUISITE: 前置条件\n" +
                "- RELATED_TO: 相关\n\n" +
                "文本：\n%s\n\n" +
                "请返回 JSON 格式：\n" +
                "{\n" +
                "  \"entities\": [{\"type\": \"PERSON\", \"name\": \"张三\", \"description\": \"后端负责人\"}],\n" +
                "  \"relations\": [{\"source\": \"张三\", \"target\": \"支付模块\", \"type\": \"RESPONSIBLE_FOR\"}]\n" +
                "}",
                text.length() > 2000 ? text.substring(0, 2000) : text
        );
    }

    /**
     * Call LLM for extraction
     */
    private String callLLM(String prompt) throws Exception {
        String url = aiProperties.getBaseUrl() + "/chat/completions";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", aiProperties.getModel());
        requestBody.put("messages", Arrays.asList(
                Map.of("role", "system", "content", "你是一个知识图谱构建助手，擅长从文本中提取实体和关系。"),
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
     * Parse LLM response and save entities and relations
     */
    private void parseAndSaveEntitiesAndRelations(Long skillId, String response) throws Exception {
        // Extract JSON from response (may have markdown code fences)
        String jsonStr = response;
        if (jsonStr.contains("```json")) {
            jsonStr = jsonStr.substring(jsonStr.indexOf("```json") + 7);
            jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
        } else if (jsonStr.contains("```")) {
            jsonStr = jsonStr.substring(jsonStr.indexOf("```") + 3);
            jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
        }

        JsonNode root = objectMapper.readTree(jsonStr.trim());

        // Save entities
        Map<String, Long> entityNameToId = new HashMap<>();
        JsonNode entitiesNode = root.path("entities");
        if (entitiesNode.isArray()) {
            for (JsonNode entityNode : entitiesNode) {
                String type = entityNode.path("type").asText();
                String name = entityNode.path("name").asText();
                String description = entityNode.path("description").asText("");

                KnowledgeEntity entity = entityRepository
                        .findBySkillIdAndEntityTypeAndEntityName(skillId, type, name)
                        .orElse(new KnowledgeEntity());

                entity.setSkillId(skillId);
                entity.setEntityType(type);
                entity.setEntityName(name);
                entity.setDescription(description);
                entity.setConfidence(0.8);

                entity = entityRepository.save(entity);
                entityNameToId.put(name, entity.getId());
            }
        }

        // Save relations
        JsonNode relationsNode = root.path("relations");
        if (relationsNode.isArray()) {
            for (JsonNode relationNode : relationsNode) {
                String source = relationNode.path("source").asText();
                String target = relationNode.path("target").asText();
                String type = relationNode.path("type").asText();

                Long sourceId = entityNameToId.get(source);
                Long targetId = entityNameToId.get(target);

                if (sourceId != null && targetId != null) {
                    KnowledgeRelation relation = new KnowledgeRelation();
                    relation.setSkillId(skillId);
                    relation.setSourceEntityId(sourceId);
                    relation.setTargetEntityId(targetId);
                    relation.setRelationType(type);
                    relation.setConfidence(0.8);

                    relationRepository.save(relation);
                }
            }
        }
    }

    /**
     * Get knowledge graph for a skill
     */
    public Map<String, Object> getKnowledgeGraph(Long skillId) {
        List<KnowledgeEntity> entities = entityRepository.findBySkillId(skillId);
        List<KnowledgeRelation> relations = relationRepository.findBySkillId(skillId);

        Map<String, Object> graph = new HashMap<>();
        graph.put("entities", entities);
        graph.put("relations", relations);
        graph.put("entityCount", entities.size());
        graph.put("relationCount", relations.size());

        return graph;
    }
}
