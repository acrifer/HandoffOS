package com.lifeos.integration.feishu.bot.service;

import com.lifeos.ai.job.dto.AiJobResponse;
import com.lifeos.ai.knowledgebase.dto.*;
import com.lifeos.ai.knowledgebase.service.AiKnowledgeService;
import com.lifeos.integration.feishu.bot.dto.FeishuBotCommand;
import com.lifeos.integration.feishu.bot.dto.FeishuBotCommandType;
import com.lifeos.integration.feishu.bot.dto.FeishuBotInboundMessage;
import com.lifeos.integration.feishu.bot.entity.FeishuBotEvent;
import com.lifeos.integration.feishu.bot.entity.FeishuChatBinding;
import com.lifeos.integration.feishu.bot.repository.FeishuBotEventRepository;
import com.lifeos.integration.feishu.bot.repository.FeishuChatBindingRepository;
import com.lifeos.skill.dto.AskSkillRequest;
import com.lifeos.skill.dto.SkillResponse;
import com.lifeos.skill.dto.SyncSourcesRequest;
import com.lifeos.skill.service.SkillService;
import com.lifeos.task.dto.TaskRequest;
import com.lifeos.task.dto.TaskResponse;
import com.lifeos.task.service.TaskService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuBotApplicationService {

    private final FeishuBotCommandService commandService;
    private final FeishuMessageService messageService;
    private final FeishuChatBindingRepository bindingRepository;
    private final FeishuBotEventRepository eventRepository;
    private final SkillService skillService;
    private final AiKnowledgeService aiKnowledgeService;
    private final TaskService taskService;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public void receive(FeishuBotInboundMessage inbound) {
        FeishuBotCommand command = commandService.parse(inbound.getRequestText());
        Optional<FeishuBotEvent> existing = eventRepository.findByEventId(inbound.getEventId());
        if (existing.isPresent()) {
            log.info("Duplicate Feishu bot event ignored: {}", inbound.getEventId());
            return;
        }

        FeishuBotEvent event = createEvent(inbound, command);
        Optional<FeishuChatBinding> binding = bindingRepository.findByChatIdAndEnabledTrue(inbound.getChatId());
        if (binding.isEmpty()) {
            failAndReply(event, inbound.getMessageId(), "请先在 HandoffOS 控制台绑定当前飞书群 chat_id 到一个 Skill。");
            return;
        }

        if (command.getType() == FeishuBotCommandType.UNKNOWN) {
            failAndReply(event, inbound.getMessageId(), helpText("指令格式无法识别。"));
            return;
        }

        if (command.requiresAsync()) {
            markProcessing(event);
            try {
                messageService.replyText(inbound.getMessageId(), "已收到，正在处理。完成后我会在当前消息下回复结果。");
            } catch (Exception e) {
                markFailed(event.getId(), e);
                return;
            }
            executor.submit(() -> executeWithReply(event.getId(), binding.get(), command, inbound.getMessageId()));
        } else {
            executeWithReply(event.getId(), binding.get(), command, inbound.getMessageId());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected FeishuBotEvent createEvent(FeishuBotInboundMessage inbound, FeishuBotCommand command) {
        FeishuBotEvent event = new FeishuBotEvent();
        event.setEventId(firstNonBlank(inbound.getEventId(), inbound.getMessageId()));
        event.setMessageId(inbound.getMessageId());
        event.setChatId(inbound.getChatId());
        event.setSenderOpenId(inbound.getSenderOpenId());
        event.setCommandType(command.getType().name());
        event.setRequestText(inbound.getRequestText());
        event.setRawPayload(inbound.getRawPayload());
        event.setStatus("RECEIVED");
        return eventRepository.save(event);
    }

    private void executeWithReply(Long eventId,
                                  FeishuChatBinding binding,
                                  FeishuBotCommand command,
                                  String messageId) {
        try {
            CommandResult result = execute(binding, command);
            messageService.replyText(messageId, result.replyText());
            markSuccess(eventId, result.jobId(), result.qaLogId());
        } catch (Exception e) {
            log.warn("Feishu bot command failed: {}", e.getMessage(), e);
            markFailed(eventId, e);
            safeReply(messageId, "处理失败：" + firstNonBlank(e.getMessage(), e.getClass().getSimpleName()));
        }
    }

    private CommandResult execute(FeishuChatBinding binding, FeishuBotCommand command) {
        Long userId = binding.getUserId();
        Long skillId = binding.getSkillId();
        return switch (command.getType()) {
            case HELP -> new CommandResult(helpText(null), null, null);
            case SYNC_RECENT -> syncRecent(userId, skillId, binding.getChatId(), command.getLimit());
            case SYNC_DOCUMENT -> syncDocument(userId, skillId, command.getDocumentRef());
            case DISTILL -> distill(userId, skillId);
            case MANUAL_DOCUMENT -> manualDocument(userId, skillId, command);
            case TASK_CREATE -> createTask(userId, skillId, command);
            case TASK_LIST -> listTasks(userId, skillId);
            case TASK_COMPLETE -> completeTask(userId, skillId, command.getTaskId());
            case STATS -> stats(skillId);
            case FEEDBACK -> feedback(userId, skillId, command);
            case ASK -> ask(userId, skillId, command.getQuestion());
            default -> throw new RuntimeException("Unsupported command");
        };
    }

    private CommandResult syncRecent(Long userId, Long skillId, String chatId, Integer limit) {
        SyncSourcesRequest request = new SyncSourcesRequest();
        request.setChatId(chatId);
        request.setLimit(limit == null ? 20 : limit);
        SkillResponse skill = skillService.syncSources(userId, skillId, request);
        String reply = "同步完成：已从当前飞书群写入 " + skill.getChatSourceCount()
                + " 条群聊来源，当前 Skill 总来源 " + skill.getSourceCount()
                + "，Dify Dataset: " + firstNonBlank(skill.getDifyDatasetId(), "未返回");
        return new CommandResult(reply, skill.getLatestJobId(), null);
    }

    private CommandResult syncDocument(Long userId, Long skillId, String documentRef) {
        SyncSourcesRequest request = new SyncSourcesRequest();
        request.setDocumentRefs(List.of(documentRef));
        request.setLimit(1);
        SkillResponse skill = skillService.syncSources(userId, skillId, request);
        String reply = "飞书文档同步完成：当前文档来源 " + skill.getDocumentSourceCount()
                + "，总来源 " + skill.getSourceCount()
                + "，Dify Dataset: " + firstNonBlank(skill.getDifyDatasetId(), "未返回");
        return new CommandResult(reply, skill.getLatestJobId(), null);
    }

    private CommandResult distill(Long userId, Long skillId) {
        AiJobResponse job = skillService.distill(userId, skillId);
        return new CommandResult("蒸馏完成：已生成交接 Skill 结构化结果。作业 ID：" + job.getId(), job.getId(), null);
    }

    private CommandResult manualDocument(Long userId, Long skillId, FeishuBotCommand command) {
        KnowledgeDocumentRequest request = new KnowledgeDocumentRequest();
        request.setTitle(command.getTitle());
        request.setSourceType("FEISHU_BOT_MANUAL");
        request.setContent(command.getContent());
        KnowledgeDocumentResponse document = aiKnowledgeService.createDocument(userId, skillId, request);
        KnowledgeDocumentResponse parsed = aiKnowledgeService.parseDocument(userId, skillId, document.getId(), new ParseDocumentRequest());
        Map<String, Object> vectorized = aiKnowledgeService.vectorizeDocument(userId, skillId, document.getId(), new VectorizeDocumentRequest());
        String reply = "资料已入库：\"" + parsed.getTitle() + "\"，chunk 数 "
                + parsed.getChunkCount() + "，Dify Document: "
                + firstNonBlank(String.valueOf(vectorized.get("difyDocumentId")), "未返回");
        return new CommandResult(reply, null, null);
    }

    private CommandResult ask(Long userId, Long skillId, String question) {
        AskSkillRequest request = new AskSkillRequest();
        request.setQuestion(question);
        QaAnswerResponse answer = aiKnowledgeService.ask(userId, skillId, request);
        StringBuilder reply = new StringBuilder();
        reply.append(answer.getAnswer());
        if (answer.getCitations() != null && !answer.getCitations().isEmpty()) {
            reply.append("\n\n引用来源：\n");
            for (int i = 0; i < answer.getCitations().size(); i++) {
                reply.append(i + 1).append(". ").append(answer.getCitations().get(i)).append("\n");
            }
        }
        reply.append("\nQA Log ID: ").append(answer.getQaLogId());
        return new CommandResult(reply.toString().trim(), answer.getJobId(), answer.getQaLogId());
    }

    private CommandResult createTask(Long userId, Long skillId, FeishuBotCommand command) {
        TaskRequest request = new TaskRequest();
        request.setTitle(command.getTitle());
        request.setDescription(command.getContent());
        request.setSkillId(skillId);
        request.setTags("feishu-bot");
        TaskResponse task = taskService.createTask(userId, request);
        return new CommandResult("行动项已创建：#" + task.getId() + " " + task.getTitle(), null, null);
    }

    private CommandResult listTasks(Long userId, Long skillId) {
        List<TaskResponse> tasks = taskService.listTasks(userId, skillId);
        if (tasks.isEmpty()) {
            return new CommandResult("当前 Skill 暂无交接行动项。", null, null);
        }
        String lines = tasks.stream()
                .limit(10)
                .map(task -> "#" + task.getId() + " [" + taskStatus(task.getStatus()) + "] " + task.getTitle())
                .collect(Collectors.joining("\n"));
        return new CommandResult("当前 Skill 行动项：\n" + lines, null, null);
    }

    private CommandResult completeTask(Long userId, Long skillId, Long taskId) {
        TaskResponse task = taskService.completeTask(userId, skillId, taskId);
        return new CommandResult("行动项已完成：#" + task.getId() + " " + task.getTitle(), null, null);
    }

    private CommandResult stats(Long skillId) {
        AdminAiStatsResponse stats = aiKnowledgeService.adminStats(LocalDateTime.now().minusDays(30), LocalDateTime.now(), skillId);
        String reply = """
                当前 Skill 近 30 天统计：
                - 问答量：%d
                - 失败问答：%d
                - 无答案：%d
                - 差评：%d
                - 机器人触发：%d
                - 机器人失败：%d
                """.formatted(
                stats.getUsage(),
                stats.getFailedCount(),
                stats.getNoAnswerCount(),
                stats.getNegativeFeedbackCount(),
                stats.getBotEventCount(),
                stats.getBotFailedEventCount()
        );
        return new CommandResult(reply.trim(), null, null);
    }

    private CommandResult feedback(Long userId, Long skillId, FeishuBotCommand command) {
        FeedbackRequest request = new FeedbackRequest();
        request.setRating(feedbackRating(command.getFeedbackLabel()));
        request.setFeedbackType(feedbackType(command.getFeedbackLabel()));
        request.setComment(command.getFeedbackComment());
        aiKnowledgeService.feedback(userId, skillId, command.getQaLogId(), request);
        return new CommandResult("反馈已记录：QA Log #" + command.getQaLogId() + " / " + feedbackType(command.getFeedbackLabel()), null, command.getQaLogId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void markProcessing(FeishuBotEvent event) {
        event.setStatus("PROCESSING");
        eventRepository.save(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void markSuccess(Long eventId, Long jobId, Long qaLogId) {
        FeishuBotEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Feishu bot event not found"));
        event.setStatus("SUCCESS");
        event.setJobId(jobId);
        event.setQaLogId(qaLogId);
        event.setErrorMessage(null);
        eventRepository.save(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void markFailed(Long eventId, Exception error) {
        eventRepository.findById(eventId).ifPresent(event -> {
            event.setStatus("FAILED");
            event.setErrorMessage(firstNonBlank(error.getMessage(), error.getClass().getSimpleName()));
            eventRepository.save(event);
        });
    }

    private void failAndReply(FeishuBotEvent event, String messageId, String text) {
        markFailed(event.getId(), new RuntimeException(text));
        safeReply(messageId, text);
    }

    private void safeReply(String messageId, String text) {
        try {
            messageService.replyText(messageId, text);
        } catch (Exception e) {
            log.warn("Failed to reply Feishu message {}: {}", messageId, e.getMessage(), e);
        }
    }

    private String helpText(String prefix) {
        String help = """
                可用指令：
                /帮助
                /同步 最近20条
                /同步 文档 <飞书文档链接>
                /蒸馏
                /资料 <标题> | <内容>
                /任务 新建 <标题> | <说明>
                /任务 列表
                /任务 完成 <任务ID>
                /统计
                /反馈 <qaLogId> 有用|缺口|错误 <备注>
                也可以直接 @我 提问，我会基于绑定 Skill 的知识库回答。
                """.trim();
        return prefix == null || prefix.isBlank() ? help : prefix + "\n\n" + help;
    }

    private int feedbackRating(String label) {
        String value = label == null ? "" : label.trim();
        if ("有用".equals(value)) {
            return 5;
        }
        if ("缺口".equals(value)) {
            return 2;
        }
        return 1;
    }

    private String feedbackType(String label) {
        String value = label == null ? "" : label.trim();
        if ("有用".equals(value)) {
            return "HELPFUL";
        }
        if ("缺口".equals(value)) {
            return "KNOWLEDGE_GAP";
        }
        return "NEEDS_FIX";
    }

    private String taskStatus(Short status) {
        if (status == null) {
            return "待处理";
        }
        return switch (status) {
            case 2 -> "已完成";
            case 1 -> "处理中";
            default -> "待处理";
        };
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank() && !"null".equalsIgnoreCase(value)) {
                return value.trim();
            }
        }
        return "";
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private record CommandResult(String replyText, Long jobId, Long qaLogId) {
    }
}
