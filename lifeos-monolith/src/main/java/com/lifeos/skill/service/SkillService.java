package com.lifeos.skill.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.ai.job.dto.AiJobResponse;
import com.lifeos.ai.job.entity.AiWorkflowJob;
import com.lifeos.ai.job.service.AiWorkflowJobService;
import com.lifeos.ai.knowledge.KnowledgeGraphService;
import com.lifeos.ai.knowledgebase.entity.AiQaLog;
import com.lifeos.ai.knowledgebase.service.AiKnowledgeService;
import com.lifeos.integration.dify.*;
import com.lifeos.integration.feishu.FeishuSourceItem;
import com.lifeos.integration.feishu.FeishuSourceService;
import com.lifeos.skill.dto.*;
import com.lifeos.skill.entity.HandoffSkill;
import com.lifeos.skill.entity.HandoffSkillChat;
import com.lifeos.skill.entity.HandoffSkillSource;
import com.lifeos.skill.repository.HandoffSkillChatRepository;
import com.lifeos.skill.repository.HandoffSkillRepository;
import com.lifeos.skill.repository.HandoffSkillSourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SkillService {

    private final HandoffSkillRepository skillRepository;
    private final HandoffSkillSourceRepository sourceRepository;
    private final HandoffSkillChatRepository chatRepository;
    private final AiWorkflowJobService jobService;
    private final FeishuSourceService feishuSourceService;
    private final DifyClient difyClient;
    private final KnowledgeGraphService knowledgeGraphService;
    private final AiKnowledgeService aiKnowledgeService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public SkillResponse create(Long userId, CreateSkillRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("Skill name is required");
        }

        HandoffSkill skill = new HandoffSkill();
        skill.setUserId(userId);
        skill.setName(request.getName().trim());
        skill.setRoleDescription(request.getRoleDescription());
        skill.setStatus("DRAFT");
        return toDetailResponse(skillRepository.save(skill));
    }

    public List<SkillResponse> list(Long userId) {
        return skillRepository.findByUserIdOrderByUpdateTimeDesc(userId)
                .stream()
                .map(this::toListResponse)
                .toList();
    }

    public SkillResponse getDetail(Long userId, Long skillId) {
        return toDetailResponse(loadSkill(userId, skillId));
    }

    @Transactional
    public SkillResponse syncSources(Long userId, Long skillId, SyncSourcesRequest request) {
        HandoffSkill skill = loadSkill(userId, skillId);
        AiWorkflowJob job = jobService.startJob(userId, skillId, "SKILL_SYNC", request);
        skill.setLatestJobId(job.getId());
        skill.setStatus("SYNCING");
        skillRepository.save(skill);

        try {
            String previousDatasetId = skill.getDifyDatasetId();
            DifyDatasetResponse dataset = difyClient.ensureDataset(
                    previousDatasetId,
                    difyDatasetName(skill),
                    skill.getRoleDescription()
            );
            skill.setDifyDatasetId(dataset.getDatasetId());
            boolean datasetRecreated = previousDatasetId != null
                    && !previousDatasetId.isBlank()
                    && !previousDatasetId.equals(dataset.getDatasetId());

            List<FeishuSourceItem> incoming = feishuSourceService.syncSources(
                    request.getDocumentRefs(),
                    request.getChatId(),
                    request.getStartTime(),
                    request.getEndTime(),
                    request.getLimit()
            );

            int indexedCount = 0;
            for (FeishuSourceItem item : incoming) {
                HandoffSkillSource source = saveOrUpdateSource(userId, skillId, item, datasetRecreated);
                if (datasetRecreated || source.getDifyDocumentId() == null || source.getDifyDocumentId().isBlank()) {
                    DifyDocumentResponse document = difyClient.upsertDocument(
                            skill.getDifyDatasetId(),
                            source.getTitle(),
                            source.getContent()
                    );
                    source.setDifyDocumentId(document.getDocumentId());
                    source.setIndexingStatus(document.getIndexingStatus());
                    sourceRepository.save(source);
                }
                aiKnowledgeService.importSource(userId, skill, source);
                indexedCount++;
            }

            refreshSkillCounters(skill);
            skill.setStatus(skill.getSourceCount() > 0 ? "SOURCES_READY" : "DRAFT");
            skill.setLastSyncTime(LocalDateTime.now());
            skillRepository.save(skill);

            Map<String, Object> result = Map.of(
                    "datasetId", skill.getDifyDatasetId(),
                    "sourceCount", skill.getSourceCount(),
                    "indexedCount", indexedCount
            );
            jobService.markSuccess(job, result, null);
            return toDetailResponse(skill);
        } catch (Exception e) {
            skill.setStatus("FAILED");
            skillRepository.save(skill);
            jobService.markFailed(job, e);
            throw e;
        }
    }

    @Transactional
    public AiJobResponse distill(Long userId, Long skillId) {
        HandoffSkill skill = loadSkill(userId, skillId);
        AiWorkflowJob job = jobService.startJob(userId, skillId, "SKILL_DISTILL", Map.of("skillId", skillId));
        skill.setLatestJobId(job.getId());
        skill.setStatus("DISTILLING");
        skillRepository.save(skill);

        try {
            if (skill.getDifyDatasetId() == null || skill.getDifyDatasetId().isBlank()) {
                DifyDatasetResponse dataset = difyClient.ensureDataset(null, difyDatasetName(skill), skill.getRoleDescription());
                skill.setDifyDatasetId(dataset.getDatasetId());
            }

            List<HandoffSkillSource> sources = sourceRepository.findBySkillId(skillId);
            List<Map<String, Object>> sourceSummaries = sources.stream().map(this::sourceAsMap).toList();
            DifyRunResponse run = difyClient.runDistillWorkflow(
                    skill.getName(),
                    skill.getRoleDescription(),
                    skill.getDifyDatasetId(),
                    sourceSummaries,
                    "user-" + userId
            );

            Map<String, Object> outputs = normalizeDistillOutputs(run.getOutputs());
            skill.setDistillResult(toJson(outputs));
            skill.setStatus("DISTILLED");
            skillRepository.save(skill);

            if (!sources.isEmpty()) {
                knowledgeGraphService.buildKnowledgeGraph(skillId, sources.stream()
                        .map(HandoffSkillSource::getContent)
                        .filter(Objects::nonNull)
                        .toList());
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("handoffSkill", outputs);
            result.put("citations", run.getCitations());
            AiWorkflowJob saved = jobService.markSuccess(job, result, run.getWorkflowRunId());
            return jobService.toResponse(saved);
        } catch (Exception e) {
            skill.setStatus("FAILED");
            skillRepository.save(skill);
            jobService.markFailed(job, e);
            throw e;
        }
    }

    @Transactional
    public AiJobResponse ask(Long userId, Long skillId, AskSkillRequest request) {
        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            throw new RuntimeException("Question is required");
        }

        HandoffSkill skill = loadSkill(userId, skillId);
        String question = request.getQuestion().trim();
        AiWorkflowJob job = jobService.startJob(userId, skillId, "SKILL_ASK", Map.of("question", question));
        skill.setLatestJobId(job.getId());
        skillRepository.save(skill);

        try {
            DifyRunResponse run = difyClient.askSkill(
                    skill.getName(),
                    skill.getDifyDatasetId(),
                    question,
                    "user-" + userId
            );

            String answer = run.getAnswer();
            if (answer == null || answer.isBlank()) {
                throw new IllegalStateException("Dify returned empty answer");
            }
            List<String> citations = run.getCitations();
            AiQaLog qaLog = aiKnowledgeService.saveQaLog(
                    userId,
                    skillId,
                    question,
                    answer,
                    citations,
                    null,
                    run.getWorkflowRunId(),
                    null,
                    "SUCCESS",
                    answer != null && answer.contains("当前知识库没有足够信息")
            );

            HandoffSkillChat chat = new HandoffSkillChat();
            chat.setUserId(userId);
            chat.setSkillId(skillId);
            chat.setJobId(job.getId());
            chat.setQuestion(question);
            chat.setAnswer(answer);
            chat.setCitations(toJson(citations));
            chat.setDifyWorkflowRunId(run.getWorkflowRunId());
            chatRepository.save(chat);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("skillAnswer", answer);
            result.put("citations", citations);
            result.put("qaLogId", qaLog.getId());
            AiWorkflowJob saved = jobService.markSuccess(job, result, run.getWorkflowRunId());
            return jobService.toResponse(saved);
        } catch (Exception e) {
            jobService.markFailed(job, e);
            throw e;
        }
    }

    public List<AiJobResponse> listJobs(Long userId, Long skillId, int limit) {
        loadSkill(userId, skillId);
        return jobService.listJobs(userId, skillId, null, limit);
    }

    private HandoffSkillSource saveOrUpdateSource(Long userId, Long skillId, FeishuSourceItem item, boolean forceReindex) {
        String contentHash = sha256(item.getContent());
        HandoffSkillSource source = sourceRepository.findBySkillIdAndContentHash(skillId, contentHash)
                .orElseGet(HandoffSkillSource::new);
        source.setSkillId(skillId);
        source.setUserId(userId);
        source.setSourceType(item.getSourceType());
        source.setExternalId(item.getExternalId());
        source.setTitle(item.getTitle());
        source.setContent(item.getContent());
        source.setContentHash(contentHash);
        source.setSourceHash(sha256(item.getSourceType() + ":" + item.getExternalId()));
        source.setSourceTime(item.getSourceTime());
        source.setLastSyncTime(LocalDateTime.now());
        if (forceReindex) {
            source.setDifyDocumentId(null);
            source.setIndexingStatus("pending");
        } else if (source.getIndexingStatus() == null) {
            source.setIndexingStatus("pending");
        }
        return sourceRepository.save(source);
    }

    private void refreshSkillCounters(HandoffSkill skill) {
        long total = sourceRepository.countBySkillId(skill.getId());
        long docs = sourceRepository.countBySkillIdAndSourceType(skill.getId(), "FEISHU_DOC");
        long chats = sourceRepository.countBySkillIdAndSourceType(skill.getId(), "FEISHU_CHAT");
        skill.setSourceCount((int) total);
        skill.setDocumentSourceCount((int) docs);
        skill.setChatSourceCount((int) chats);
    }

    private HandoffSkill loadSkill(Long userId, Long skillId) {
        return skillRepository.findByIdAndUserId(skillId, userId)
                .orElseThrow(() -> new RuntimeException("Skill not found"));
    }

    private String difyDatasetName(HandoffSkill skill) {
        String prefix = "HandoffOS-" + skill.getId() + "-";
        String rawName = skill.getName() == null ? "Skill" : skill.getName().replaceAll("\\s+", "");
        int maxNameLength = Math.max(1, 40 - prefix.length());
        String safeName = rawName.length() > maxNameLength ? rawName.substring(0, maxNameLength) : rawName;
        return prefix + safeName;
    }

    private SkillResponse toListResponse(HandoffSkill skill) {
        SkillResponse response = baseResponse(skill);
        return response;
    }

    private SkillResponse toDetailResponse(HandoffSkill skill) {
        SkillResponse response = baseResponse(skill);
        response.setSources(sourceRepository.findBySkillIdOrderByCreateTimeDesc(skill.getId())
                .stream()
                .map(this::toSourceResponse)
                .toList());
        response.setChats(chatRepository.findBySkillIdOrderByCreateTimeDesc(skill.getId())
                .stream()
                .map(this::toChatResponse)
                .toList());
        return response;
    }

    private SkillResponse baseResponse(HandoffSkill skill) {
        SkillResponse response = new SkillResponse();
        response.setId(skill.getId());
        response.setName(skill.getName());
        response.setRoleDescription(skill.getRoleDescription());
        response.setStatus(skill.getStatus());
        response.setDistillResult(parseMap(skill.getDistillResult()));
        response.setSourceCount(skill.getSourceCount());
        response.setDocumentSourceCount(skill.getDocumentSourceCount());
        response.setChatSourceCount(skill.getChatSourceCount());
        response.setLatestJobId(skill.getLatestJobId());
        response.setDifyDatasetId(skill.getDifyDatasetId());
        response.setLastSyncTime(skill.getLastSyncTime());
        response.setCreateTime(skill.getCreateTime());
        response.setUpdateTime(skill.getUpdateTime());
        return response;
    }

    private SkillSourceResponse toSourceResponse(HandoffSkillSource source) {
        SkillSourceResponse response = new SkillSourceResponse();
        response.setId(source.getId());
        response.setSourceType(source.getSourceType());
        response.setExternalId(source.getExternalId());
        response.setTitle(source.getTitle());
        response.setContentPreview(preview(source.getContent(), 180));
        response.setDifyDocumentId(source.getDifyDocumentId());
        response.setIndexingStatus(source.getIndexingStatus());
        response.setSourceTime(source.getSourceTime());
        response.setCreateTime(source.getCreateTime());
        return response;
    }

    private SkillChatResponse toChatResponse(HandoffSkillChat chat) {
        SkillChatResponse response = new SkillChatResponse();
        response.setId(chat.getId());
        response.setJobId(chat.getJobId());
        response.setQuestion(chat.getQuestion());
        response.setAnswer(chat.getAnswer());
        response.setCitations(parseStringList(chat.getCitations()));
        response.setDifyWorkflowRunId(chat.getDifyWorkflowRunId());
        response.setCreateTime(chat.getCreateTime());
        return response;
    }

    private Map<String, Object> sourceAsMap(HandoffSkillSource source) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", source.getId());
        map.put("sourceType", source.getSourceType());
        map.put("externalId", source.getExternalId());
        map.put("title", source.getTitle());
        map.put("contentPreview", preview(source.getContent(), 600));
        map.put("difyDocumentId", source.getDifyDocumentId());
        map.put("indexingStatus", source.getIndexingStatus());
        return map;
    }

    private Map<String, Object> normalizeDistillOutputs(Map<String, Object> outputs) {
        if (outputs == null || outputs.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Object handoffSkill = outputs.get("handoffSkill");
        if (handoffSkill instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        return new LinkedHashMap<>(outputs);
    }

    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of("raw", json);
        }
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of(json);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private String preview(String content, int max) {
        if (content == null) {
            return "";
        }
        String compact = content.replaceAll("\\s+", " ").trim();
        return compact.length() > max ? compact.substring(0, max) + "..." : compact;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return Integer.toHexString(String.valueOf(value).hashCode());
        }
    }
}
