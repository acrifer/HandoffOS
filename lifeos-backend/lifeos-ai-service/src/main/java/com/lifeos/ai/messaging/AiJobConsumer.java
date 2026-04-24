package com.lifeos.ai.messaging;

import com.alibaba.fastjson2.JSON;
import com.lifeos.ai.service.AiSummaryService;
import com.lifeos.api.ai.client.NoteFeignClient;
import com.lifeos.api.ai.dto.AiAsyncJobCommand;
import com.lifeos.api.ai.dto.AiAsyncJobResultDTO;
import com.lifeos.api.ai.dto.AiAsyncJobUpdateDTO;
import com.lifeos.api.ai.dto.AiJobStatus;
import com.lifeos.api.ai.dto.AiJobType;
import com.lifeos.api.ai.dto.AiSummaryCommand;
import com.lifeos.api.ai.dto.AiSummaryResult;
import com.lifeos.api.ai.dto.AiNoteOrganizeResult;
import com.lifeos.api.ai.dto.AiTaskExtractionResult;
import com.lifeos.api.ai.mq.AiMqConstants;
import com.lifeos.common.response.Result;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RocketMQMessageListener(topic = AiMqConstants.TOPIC, consumerGroup = AiMqConstants.CONSUMER_GROUP)
public class AiJobConsumer implements RocketMQListener<String> {

    @Resource
    private AiSummaryService aiSummaryService;

    @Resource
    private NoteFeignClient noteFeignClient;

    @Override
    public void onMessage(String message) {
        AiAsyncJobCommand command = JSON.parseObject(message, AiAsyncJobCommand.class);
        if (command == null || command.getJobId() == null) {
            log.warn("Ignored empty ai job message");
            return;
        }

        log.info("Received ai job {} with type {}", command.getJobId(), command.getJobType());
        updateStatus(command.getJobId(), AiJobStatus.PROCESSING, null, null);
        try {
            AiAsyncJobResultDTO result = switch (command.getJobType()) {
                case AiJobType.SUMMARY -> buildSummaryResult(command);
                case AiJobType.ORGANIZE -> buildOrganizeResult(command);
                case AiJobType.EXTRACT_TASKS -> buildTaskExtractionResult(command);
                case AiJobType.WEEKLY_REVIEW -> buildWeeklyReviewResult(command);
                case AiJobType.SKILL_DISTILL -> buildSkillDistillResult(command);
                case AiJobType.SKILL_ASK -> buildSkillAskResult(command);
                default -> throw new IllegalArgumentException("Unsupported ai job type: " + command.getJobType());
            };
            updateStatus(command.getJobId(), AiJobStatus.SUCCESS, null, result);
            log.info("Ai job {} completed successfully", command.getJobId());
        } catch (Exception ex) {
            log.warn("Failed to process ai job {}", command.getJobId(), ex);
            updateStatus(command.getJobId(), AiJobStatus.FAILED, ex.getMessage(), null);
            throw new RuntimeException(ex);
        }
    }

    private AiAsyncJobResultDTO buildSummaryResult(AiAsyncJobCommand command) {
        AiSummaryResult summaryResult = aiSummaryService.generateSummary(toSummaryCommand(command));
        if (!AiJobStatus.SUCCESS.equalsIgnoreCase(summaryResult.getStatus())) {
            throw new RuntimeException(summaryResult.getErrorMessage());
        }
        AiAsyncJobResultDTO result = new AiAsyncJobResultDTO();
        result.setSummary(summaryResult.getSummary());
        return result;
    }

    private AiAsyncJobResultDTO buildOrganizeResult(AiAsyncJobCommand command) {
        AiNoteOrganizeResult organizeResult = aiSummaryService.organizeNote(toSummaryCommand(command));
        if (!AiJobStatus.SUCCESS.equalsIgnoreCase(organizeResult.getStatus())) {
            throw new RuntimeException(organizeResult.getErrorMessage());
        }
        AiAsyncJobResultDTO result = new AiAsyncJobResultDTO();
        result.setSummary(organizeResult.getSummary());
        result.setSuggestedTitle(organizeResult.getSuggestedTitle());
        result.setSuggestedTags(organizeResult.getSuggestedTags());
        return result;
    }

    private AiAsyncJobResultDTO buildTaskExtractionResult(AiAsyncJobCommand command) {
        AiTaskExtractionResult extractionResult = aiSummaryService.extractTasks(toSummaryCommand(command));
        if (!AiJobStatus.SUCCESS.equalsIgnoreCase(extractionResult.getStatus())) {
            throw new RuntimeException(extractionResult.getErrorMessage());
        }
        AiAsyncJobResultDTO result = new AiAsyncJobResultDTO();
        result.setTasks(extractionResult.getTasks());
        return result;
    }

    private AiAsyncJobResultDTO buildWeeklyReviewResult(AiAsyncJobCommand command) {
        AiAsyncJobResultDTO result = new AiAsyncJobResultDTO();
        result.setWeeklyReview(aiSummaryService.generateWeeklyReview(command));
        return result;
    }

    private AiAsyncJobResultDTO buildSkillDistillResult(AiAsyncJobCommand command) {
        AiAsyncJobResultDTO result = new AiAsyncJobResultDTO();
        result.setHandoffSkill(aiSummaryService.distillHandoffSkill(command));
        return result;
    }

    private AiAsyncJobResultDTO buildSkillAskResult(AiAsyncJobCommand command) {
        AiAsyncJobResultDTO result = new AiAsyncJobResultDTO();
        result.setSkillAnswer(aiSummaryService.answerHandoffSkillQuestion(command));
        if (command.getHandoffSkill() != null && command.getHandoffSkill().getCitations() != null) {
            result.setCitations(command.getHandoffSkill().getCitations().stream().limit(5).toList());
        }
        return result;
    }

    private AiSummaryCommand toSummaryCommand(AiAsyncJobCommand command) {
        AiSummaryCommand summaryCommand = new AiSummaryCommand();
        summaryCommand.setNoteId(command.getNoteId());
        summaryCommand.setUserId(command.getUserId());
        summaryCommand.setTitle(command.getTitle());
        summaryCommand.setContent(command.getContent());
        return summaryCommand;
    }

    private void updateStatus(Long jobId, String status, String errorMessage, AiAsyncJobResultDTO result) {
        AiAsyncJobUpdateDTO update = new AiAsyncJobUpdateDTO();
        update.setStatus(status);
        update.setErrorMessage(errorMessage);
        update.setResult(result);
        Result<Void> response = noteFeignClient.updateJobStatus(jobId, update);
        if (response == null || response.getCode() == null || response.getCode() != 200) {
            String message = response == null ? "No response" : response.getMessage();
            throw new RuntimeException("Failed to update ai job status: " + message);
        }
    }
}
