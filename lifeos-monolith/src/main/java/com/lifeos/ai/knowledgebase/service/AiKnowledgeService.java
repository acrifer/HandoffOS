package com.lifeos.ai.knowledgebase.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.ai.job.entity.AiWorkflowJob;
import com.lifeos.ai.job.service.AiWorkflowJobService;
import com.lifeos.ai.knowledgebase.dto.*;
import com.lifeos.ai.knowledgebase.entity.*;
import com.lifeos.ai.knowledgebase.repository.*;
import com.lifeos.demo.exception.ApiException;
import com.lifeos.demo.service.DemoDeviceService;
import com.lifeos.integration.dify.DifyClient;
import com.lifeos.integration.dify.DifyDatasetResponse;
import com.lifeos.integration.dify.DifyDocumentResponse;
import com.lifeos.integration.dify.DifyRunResponse;
import com.lifeos.integration.feishu.bot.entity.FeishuBotEvent;
import com.lifeos.integration.feishu.bot.entity.FeishuChatBinding;
import com.lifeos.integration.feishu.bot.repository.FeishuBotEventRepository;
import com.lifeos.integration.feishu.bot.repository.FeishuChatBindingRepository;
import com.lifeos.skill.dto.AskSkillRequest;
import com.lifeos.skill.entity.HandoffSkill;
import com.lifeos.skill.entity.HandoffSkillSource;
import com.lifeos.skill.repository.HandoffSkillRepository;
import com.lifeos.skill.repository.HandoffSkillSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiKnowledgeService {

    private static final int CHUNK_SIZE = 700;
    private static final int CHUNK_OVERLAP = 100;

    private final HandoffSkillRepository skillRepository;
    private final HandoffSkillSourceRepository sourceRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final VectorIndexMappingRepository mappingRepository;
    private final AiQaLogRepository qaLogRepository;
    private final AiFeedbackRepository feedbackRepository;
    private final PromptTemplateRepository promptTemplateRepository;
    private final AiWorkflowJobService jobService;
    private final DifyClient difyClient;
    private final PlatformTransactionManager transactionManager;
    private final FeishuBotEventRepository feishuBotEventRepository;
    private final FeishuChatBindingRepository feishuChatBindingRepository;
    private final DemoDeviceService demoDeviceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public KnowledgeDocumentResponse createDocument(Long userId, Long skillId, KnowledgeDocumentRequest request) {
        HandoffSkill skill = loadSkill(userId, skillId);
        String title = firstNonBlank(request.getTitle(), "手动上传资料");
        String content = firstNonBlank(request.getContent(), request.getSourceUrl(), title);

        KnowledgeDocument document = new KnowledgeDocument();
        document.setUserId(userId);
        document.setSkillId(skill.getId());
        document.setTitle(title);
        document.setSourceType(firstNonBlank(request.getSourceType(), "MANUAL_TEXT"));
        document.setSourceUrl(request.getSourceUrl());
        document.setRawContent(content);
        document.setStatus("UPLOADED");
        return toDocumentResponse(documentRepository.save(document), false);
    }

    @Transactional
    public KnowledgeDocumentResponse importSource(Long userId, HandoffSkill skill, HandoffSkillSource source) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setUserId(userId);
        document.setSkillId(skill.getId());
        document.setTitle(firstNonBlank(source.getTitle(), source.getExternalId(), "飞书来源"));
        document.setSourceType(source.getSourceType());
        document.setSourceUrl(source.getExternalId());
        document.setDifyDocumentId(source.getDifyDocumentId());
        document.setStatus("INDEXED");
        document.setRawContent(source.getContent());
        document.setSummary(buildSummary(document.getTitle(), document.getSourceType(), document.getRawContent()));
        document = documentRepository.save(document);
        replaceChunks(document);
        createMappings(document, skill.getDifyDatasetId(), source.getDifyDocumentId(), "dify-managed", "INDEXED");
        return toDocumentResponse(document, true);
    }

    @Transactional
    public KnowledgeDocumentResponse parseDocument(Long userId, Long skillId, Long documentId, ParseDocumentRequest request) {
        loadSkill(userId, skillId);
        KnowledgeDocument document = loadDocument(skillId, documentId);
        replaceChunks(document);
        document.setSummary(buildSummary(document.getTitle(), document.getSourceType(), document.getRawContent()));
        document.setStatus("PARSED");
        return toDocumentResponse(documentRepository.save(document), true);
    }

    @Transactional
    public Map<String, Object> vectorizeDocument(Long userId, Long skillId, Long documentId, VectorizeDocumentRequest request) {
        HandoffSkill skill = loadSkill(userId, skillId);
        KnowledgeDocument document = loadDocument(skillId, documentId);
        if (skill.getDifyDatasetId() == null || skill.getDifyDatasetId().isBlank()) {
            DifyDatasetResponse dataset = difyClient.ensureDataset(null, skill.getName(), skill.getRoleDescription());
            skill.setDifyDatasetId(dataset.getDatasetId());
            skillRepository.save(skill);
        }
        if (chunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId).isEmpty()) {
            replaceChunks(document);
        }

        long estimatedTokens = demoDeviceService.estimateTokens(document.getTitle(), document.getRawContent(), document.getSummary());
        demoDeviceService.requireAvailable(userId, "DOCUMENT_VECTORIZE", estimatedTokens);
        DifyDocumentResponse difyDocument = difyClient.upsertDocument(
                skill.getDifyDatasetId(),
                document.getTitle(),
                firstNonBlank(document.getRawContent(), document.getSummary(), document.getTitle())
        );
        document.setDifyDocumentId(difyDocument.getDocumentId());
        document.setStatus(difyDocument.getIndexingStatus() == null ? "INDEXING" : difyDocument.getIndexingStatus().toUpperCase(Locale.ROOT));
        documentRepository.save(document);

        createMappings(document, skill.getDifyDatasetId(), difyDocument.getDocumentId(),
                firstNonBlank(request.getEmbeddingModel(), "dify-managed"), document.getStatus());
        demoDeviceService.recordUsage(userId, skillId, "DIFY_KNOWLEDGE", "DOCUMENT_VECTORIZE",
                estimatedTokens, 0L, true, difyDocument.getDocumentId(), "SUCCESS");
        return Map.of(
                "documentId", documentId,
                "difyDatasetId", skill.getDifyDatasetId(),
                "difyDocumentId", difyDocument.getDocumentId(),
                "indexStatus", document.getStatus()
        );
    }

    @Transactional
    public KnowledgeDocumentResponse summarizeSkill(Long userId, Long skillId) {
        loadSkill(userId, skillId);
        List<KnowledgeDocument> documents = documentRepository.findBySkillIdOrderByCreateTimeDesc(skillId);
        if (documents.isEmpty()) {
            List<HandoffSkillSource> sources = sourceRepository.findBySkillId(skillId);
            if (sources.isEmpty()) {
                throw new RuntimeException("No knowledge documents or sources found");
            }
            HandoffSkill skill = loadSkill(userId, skillId);
            for (HandoffSkillSource source : sources) {
                importSource(userId, skill, source);
            }
            documents = documentRepository.findBySkillIdOrderByCreateTimeDesc(skillId);
        }
        KnowledgeDocument latest = documents.get(0);
        latest.setSummary(buildSummary(latest.getTitle(), latest.getSourceType(), latest.getRawContent()));
        latest.setStatus("SUMMARIZED");
        return toDocumentResponse(documentRepository.save(latest), true);
    }

    @Transactional
    public QaAnswerResponse ask(Long userId, Long skillId, AskSkillRequest request) {
        HandoffSkill skill = loadSkill(userId, skillId);
        String question = request.getQuestion() == null ? "" : request.getQuestion().trim();
        if (question.isEmpty()) {
            throw new RuntimeException("Question is required");
        }

        long started = System.nanoTime();
        AiWorkflowJob job = jobService.startJob(userId, skillId, "SKILL_ASK", Map.of("question", question));
        try {
            long estimatedTokens = demoDeviceService.estimateTokens(question);
            demoDeviceService.requireAvailable(userId, "KNOWLEDGE_ASK", estimatedTokens);
            DifyRunResponse run = difyClient.askSkill(skill.getName(), skill.getDifyDatasetId(), question, "user-" + userId);
            String answer = firstNonBlank(run.getAnswer());
            if (answer.isBlank()) {
                throw new IllegalStateException("Dify returned empty answer");
            }
            List<String> citations = run.getCitations();
            boolean noAnswer = isNoAnswer(answer);
            int latencyMs = (int) Duration.ofNanos(System.nanoTime() - started).toMillis();

            AiQaLog log = saveQaLog(userId, skillId, question, answer, citations, null,
                    run.getWorkflowRunId(), latencyMs, "SUCCESS", noAnswer);
            AiWorkflowJob saved = jobService.markSuccess(job, Map.of(
                    "skillAnswer", answer,
                    "citations", citations,
                    "qaLogId", log.getId(),
                    "noAnswer", noAnswer
            ), run.getWorkflowRunId());
            long requestTokens = run.getRequestTokens() == null || run.getRequestTokens() == 0L
                    ? estimatedTokens
                    : run.getRequestTokens();
            long responseTokens = run.getResponseTokens() == null || run.getResponseTokens() == 0L
                    ? demoDeviceService.estimateTokens(answer)
                    : run.getResponseTokens();
            demoDeviceService.recordUsage(userId, skillId, "DIFY_CHATFLOW", "KNOWLEDGE_ASK",
                    requestTokens, responseTokens, run.isUsageEstimated(), run.getWorkflowRunId(), "SUCCESS");

            QaAnswerResponse response = new QaAnswerResponse();
            response.setAnswer(answer);
            response.setCitations(citations);
            response.setQaLogId(log.getId());
            response.setJobId(saved.getId());
            response.setConversationId(log.getConversationId());
            response.setDifyWorkflowRunId(run.getWorkflowRunId());
            response.setNoAnswer(noAnswer);
            return response;
        } catch (Exception e) {
            int latencyMs = (int) Duration.ofNanos(System.nanoTime() - started).toMillis();
            saveQaLogRequiresNew(userId, skillId, question, "", List.of(), null, null, latencyMs, "FAILED", false);
            jobService.markFailed(job, e);
            if (!(e instanceof ApiException)) {
                demoDeviceService.recordUsage(userId, skillId, "DIFY_CHATFLOW", "KNOWLEDGE_ASK",
                        1L, 0L, true, null, "FAILED");
            }
            throw e;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiQaLog saveQaLog(Long userId,
                             Long skillId,
                             String question,
                             String answer,
                             List<String> citations,
                             String conversationId,
                             String difyWorkflowRunId,
                             Integer latencyMs,
                             String status,
                             boolean noAnswer) {
        AiQaLog log = new AiQaLog();
        log.setUserId(userId);
        log.setSkillId(skillId);
        log.setQuestion(question);
        log.setAnswer(answer);
        log.setCitations(toJson(citations));
        log.setConversationId(conversationId);
        log.setDifyWorkflowRunId(difyWorkflowRunId);
        log.setLatencyMs(latencyMs);
        log.setStatus(status);
        log.setNoAnswer(noAnswer);
        return qaLogRepository.save(log);
    }

    private void saveQaLogRequiresNew(Long userId,
                                      Long skillId,
                                      String question,
                                      String answer,
                                      List<String> citations,
                                      String conversationId,
                                      String difyWorkflowRunId,
                                      Integer latencyMs,
                                      String status,
                                      boolean noAnswer) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.executeWithoutResult(tx -> saveQaLog(
                userId,
                skillId,
                question,
                answer,
                citations,
                conversationId,
                difyWorkflowRunId,
                latencyMs,
                status,
                noAnswer
        ));
    }

    public List<QaLogResponse> history(Long userId, Long skillId, int page, int size, String status) {
        loadSkill(userId, skillId);
        List<AiQaLog> logs = qaLogRepository.findByUserIdAndSkillIdOrderByCreateTimeDesc(
                userId,
                skillId,
                PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)))
        );
        return logs.stream()
                .filter(log -> status == null || status.isBlank() || status.equalsIgnoreCase(log.getStatus()))
                .map(this::toQaLogResponse)
                .toList();
    }

    @Transactional
    public Map<String, Object> feedback(Long userId, Long qaLogId, FeedbackRequest request) {
        AiQaLog log = qaLogRepository.findById(qaLogId)
                .orElseThrow(() -> new RuntimeException("QA log not found"));
        loadSkill(userId, log.getSkillId());
        return saveFeedback(userId, qaLogId, request);
    }

    @Transactional
    public Map<String, Object> feedback(Long userId, Long skillId, Long qaLogId, FeedbackRequest request) {
        AiQaLog log = qaLogRepository.findById(qaLogId)
                .orElseThrow(() -> new RuntimeException("QA log not found"));
        loadSkill(userId, skillId);
        if (!skillId.equals(log.getSkillId())) {
            throw new RuntimeException("QA log does not belong to the bound Skill");
        }
        return saveFeedback(userId, qaLogId, request);
    }

    private Map<String, Object> saveFeedback(Long userId, Long qaLogId, FeedbackRequest request) {
        AiFeedback feedback = new AiFeedback();
        feedback.setQaLogId(qaLogId);
        feedback.setUserId(userId);
        feedback.setRating(request.getRating() == null ? 0 : request.getRating());
        feedback.setFeedbackType(firstNonBlank(request.getFeedbackType(), feedback.getRating() >= 4 ? "HELPFUL" : "NEEDS_FIX"));
        feedback.setComment(request.getComment());
        feedback = feedbackRepository.save(feedback);
        return Map.of("feedbackId", feedback.getId(), "success", true);
    }

    public List<SearchResultResponse> search(Long userId, Long skillId, String query, int limit) {
        loadSkill(userId, skillId);
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        int max = Math.max(1, Math.min(limit, 20));
        return chunkRepository.findBySkillIdOrderByCreateTimeDesc(skillId)
                .stream()
                .map(chunk -> scoreChunk(chunk, normalized))
                .filter(result -> result.getScore() > 0 || normalized.isEmpty())
                .sorted(Comparator.comparing(SearchResultResponse::getScore).reversed())
                .limit(max)
                .toList();
    }

    public List<RecommendedQuestionResponse> recommendedQuestions(Long userId, Long skillId) {
        HandoffSkill skill = loadSkill(userId, skillId);
        List<String> questions = extractDistillQuestions(skill.getDistillResult());
        return questions.stream()
                .limit(8)
                .map(question -> new RecommendedQuestionResponse(question, classifyQuestion(question)))
                .toList();
    }

    public AdminAiStatsResponse adminStats(LocalDateTime start, LocalDateTime end, Long skillId) {
        LocalDateTime safeStart = start == null ? LocalDateTime.now().minusDays(30) : start;
        LocalDateTime safeEnd = end == null ? LocalDateTime.now() : end;
        List<AiQaLog> logs = qaLogRepository.findByCreateTimeBetweenOrderByCreateTimeDesc(safeStart, safeEnd);
        if (skillId != null) {
            logs = logs.stream().filter(log -> skillId.equals(log.getSkillId())).toList();
        }
        long usage = logs.size();
        long failed = logs.stream().filter(log -> "FAILED".equalsIgnoreCase(log.getStatus())).count();
        long noAnswer = logs.stream().filter(log -> Boolean.TRUE.equals(log.getNoAnswer())).count();
        long negative = feedbackRepository.countByRatingLessThanEqualAndCreateTimeBetween(2, safeStart, safeEnd);

        AdminAiStatsResponse response = new AdminAiStatsResponse();
        response.setUsage(usage);
        response.setFailedCount(failed);
        response.setNoAnswerCount(noAnswer);
        response.setNegativeFeedbackCount(negative);
        response.setNoAnswerRate(usage == 0 ? 0.0 : (double) noAnswer / usage);
        response.setTopQuestions(topQuestions(logs));
        List<FeishuBotEvent> botEvents = feishuBotEventRepository.findByCreateTimeBetweenOrderByCreateTimeDesc(safeStart, safeEnd);
        long botBindingCount = feishuChatBindingRepository.countByEnabledTrue();
        if (skillId != null) {
            List<String> chatIds = feishuChatBindingRepository.findBySkillIdAndEnabledTrue(skillId)
                    .stream()
                    .map(FeishuChatBinding::getChatId)
                    .toList();
            botEvents = botEvents.stream().filter(event -> chatIds.contains(event.getChatId())).toList();
            botBindingCount = chatIds.size();
        }
        response.setBotEventCount((long) botEvents.size());
        response.setBotFailedEventCount(botEvents.stream().filter(event -> "FAILED".equalsIgnoreCase(event.getStatus())).count());
        response.setBotBindingCount(botBindingCount);
        response.setBotCommandDistribution(botEvents.stream()
                .collect(Collectors.groupingBy(
                        event -> firstNonBlank(event.getCommandType(), "UNKNOWN"),
                        LinkedHashMap::new,
                        Collectors.counting()
                )));
        return response;
    }

    public LogAnalysisResponse analyzeLogs(LogAnalysisRequest request) {
        LocalDateTime start = request.getStart() == null ? LocalDateTime.now().minusDays(30) : request.getStart();
        LocalDateTime end = request.getEnd() == null ? LocalDateTime.now() : request.getEnd();
        List<AiQaLog> logs = qaLogRepository.findByCreateTimeBetweenOrderByCreateTimeDesc(start, end);
        if (request.getSkillId() != null) {
            logs = logs.stream().filter(log -> request.getSkillId().equals(log.getSkillId())).toList();
        }

        LogAnalysisResponse response = new LogAnalysisResponse();
        response.setHighFrequencyQuestions(topQuestions(logs));
        response.setKnowledgeGaps(logs.stream()
                .filter(log -> Boolean.TRUE.equals(log.getNoAnswer()))
                .map(AiQaLog::getQuestion)
                .limit(8)
                .toList());
        response.setPromptSuggestions(List.of(
                "继续要求答案必须引用来源编号",
                "对知识库无依据的问题保持明确拒答",
                "把差评问题对应的飞书资料补入 Skill 知识库"
        ));
        response.setSummary("共分析 " + logs.size() + " 条问答记录，发现 "
                + response.getKnowledgeGaps().size() + " 类无答案问题。");
        return response;
    }

    public List<KnowledgeDocumentResponse> listDocuments(Long userId, Long skillId) {
        loadSkill(userId, skillId);
        return documentRepository.findBySkillIdOrderByCreateTimeDesc(skillId)
                .stream()
                .map(document -> toDocumentResponse(document, false))
                .toList();
    }

    private void replaceChunks(KnowledgeDocument document) {
        chunkRepository.deleteByDocumentId(document.getId());
        List<String> parts = splitIntoChunks(firstNonBlank(document.getRawContent(), document.getSummary(), document.getTitle()));
        for (int i = 0; i < parts.size(); i++) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setDocumentId(document.getId());
            chunk.setSkillId(document.getSkillId());
            chunk.setChunkIndex(i);
            chunk.setContent(parts.get(i));
            chunk.setSourceTitle(document.getTitle());
            chunk.setSourceLocator(document.getSourceType() + "#" + (i + 1));
            chunkRepository.save(chunk);
        }
    }

    private void createMappings(KnowledgeDocument document, String datasetId, String difyDocumentId, String model, String status) {
        List<KnowledgeChunk> chunks = chunkRepository.findByDocumentIdOrderByChunkIndexAsc(document.getId());
        for (KnowledgeChunk chunk : chunks) {
            VectorIndexMapping mapping = new VectorIndexMapping();
            mapping.setChunkId(chunk.getId());
            mapping.setDifyDatasetId(datasetId);
            mapping.setDifyDocumentId(difyDocumentId);
            mapping.setEmbeddingModel(model);
            mapping.setIndexStatus(status);
            mappingRepository.save(mapping);
        }
    }

    private List<String> splitIntoChunks(String text) {
        String compact = text == null ? "" : text.replaceAll("\\r\\n", "\n").replaceAll("[ \\t]+", " ").trim();
        if (compact.isBlank()) {
            return List.of("暂无可解析文本。");
        }
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < compact.length()) {
            int end = Math.min(compact.length(), start + CHUNK_SIZE);
            chunks.add(compact.substring(start, end).trim());
            if (end == compact.length()) {
                break;
            }
            start = Math.max(0, end - CHUNK_OVERLAP);
        }
        return chunks;
    }

    private String buildSummary(String title, String sourceType, String content) {
        String preview = preview(content, 900);
        return """
                - 核心背景：%s 来自 %s，已纳入团队交接知识库。
                - 关键流程：优先阅读资料中的步骤、检查项和负责人描述。
                - 角色与责任：根据资料原文确认责任边界，避免口头经验失真。
                - 决策规则：涉及上线、回滚、权限和风险操作时必须回到来源核对。
                - 风险点：资料缺失或过期时，AI 应提示补充资料而不是编造。
                - 新人需要优先阅读/确认的问题：%s
                """.formatted(firstNonBlank(title, "交接资料"), firstNonBlank(sourceType, "UNKNOWN"), preview);
    }

    private SearchResultResponse scoreChunk(KnowledgeChunk chunk, String query) {
        String content = chunk.getContent() == null ? "" : chunk.getContent();
        String haystack = (content + " " + chunk.getSourceTitle()).toLowerCase(Locale.ROOT);
        double score = query == null || query.isBlank() ? 0.1 : 0.0;
        if (query != null && !query.isBlank()) {
            for (String token : query.split("\\s+")) {
                if (!token.isBlank() && haystack.contains(token)) {
                    score += 1.0;
                }
            }
            if (haystack.contains(query)) {
                score += 2.0;
            }
        }
        SearchResultResponse response = new SearchResultResponse();
        response.setChunkId(chunk.getId());
        response.setDocumentId(chunk.getDocumentId());
        response.setSourceTitle(chunk.getSourceTitle());
        response.setSourceLocator(chunk.getSourceLocator());
        response.setContentPreview(preview(content, 260));
        response.setScore(score);
        return response;
    }

    private List<String> topQuestions(List<AiQaLog> logs) {
        return logs.stream()
                .collect(Collectors.groupingBy(AiQaLog::getQuestion, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .map(Map.Entry::getKey)
                .toList();
    }

    private KnowledgeDocument loadDocument(Long skillId, Long documentId) {
        return documentRepository.findByIdAndSkillId(documentId, skillId)
                .orElseThrow(() -> new RuntimeException("Knowledge document not found"));
    }

    private HandoffSkill loadSkill(Long userId, Long skillId) {
        return skillRepository.findByIdAndUserId(skillId, userId)
                .orElseThrow(() -> new RuntimeException("Skill not found"));
    }

    private KnowledgeDocumentResponse toDocumentResponse(KnowledgeDocument document, boolean includeChunks) {
        KnowledgeDocumentResponse response = new KnowledgeDocumentResponse();
        response.setId(document.getId());
        response.setSkillId(document.getSkillId());
        response.setTitle(document.getTitle());
        response.setSourceType(document.getSourceType());
        response.setSourceUrl(document.getSourceUrl());
        response.setDifyDocumentId(document.getDifyDocumentId());
        response.setStatus(document.getStatus());
        response.setSummary(document.getSummary());
        List<KnowledgeChunk> chunks = chunkRepository.findByDocumentIdOrderByChunkIndexAsc(document.getId());
        response.setChunkCount(chunks.size());
        if (includeChunks) {
            response.setChunks(chunks.stream().map(this::toChunkResponse).toList());
        }
        response.setCreateTime(document.getCreateTime());
        response.setUpdateTime(document.getUpdateTime());
        return response;
    }

    private KnowledgeChunkResponse toChunkResponse(KnowledgeChunk chunk) {
        KnowledgeChunkResponse response = new KnowledgeChunkResponse();
        response.setId(chunk.getId());
        response.setChunkIndex(chunk.getChunkIndex());
        response.setContentPreview(preview(chunk.getContent(), 260));
        response.setSourceTitle(chunk.getSourceTitle());
        response.setSourceLocator(chunk.getSourceLocator());
        return response;
    }

    private QaLogResponse toQaLogResponse(AiQaLog log) {
        QaLogResponse response = new QaLogResponse();
        response.setId(log.getId());
        response.setSkillId(log.getSkillId());
        response.setQuestion(log.getQuestion());
        response.setAnswer(log.getAnswer());
        response.setCitations(parseStringList(log.getCitations()));
        response.setConversationId(log.getConversationId());
        response.setDifyWorkflowRunId(log.getDifyWorkflowRunId());
        response.setLatencyMs(log.getLatencyMs());
        response.setStatus(log.getStatus());
        response.setNoAnswer(log.getNoAnswer());
        response.setCreateTime(log.getCreateTime());
        return response;
    }

    private List<String> extractDistillQuestions(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
            Object questions = map.get("handoffQuestions");
            if (questions instanceof List<?> list) {
                return list.stream().map(String::valueOf).toList();
            }
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
        return new ArrayList<>();
    }

    private String classifyQuestion(String question) {
        if (question.contains("上线") || question.contains("发布")) return "上线检查";
        if (question.contains("风险") || question.contains("确认")) return "风险控制";
        if (question.contains("谁") || question.contains("联系")) return "负责人";
        if (question.contains("第一天") || question.contains("新人")) return "新人上手";
        return "交接问答";
    }

    private boolean isNoAnswer(String answer) {
        String value = answer == null ? "" : answer;
        return value.contains("当前知识库没有足够信息") || value.toLowerCase(Locale.ROOT).contains("not enough information");
    }

    private String preview(String content, int max) {
        if (content == null) {
            return "";
        }
        String compact = content.replaceAll("\\s+", " ").trim();
        return compact.length() > max ? compact.substring(0, max) + "..." : compact;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
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
}
