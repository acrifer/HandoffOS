package com.lifeos.note.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.api.ai.dto.AiAsyncJobCommand;
import com.lifeos.api.ai.dto.AiAsyncJobDTO;
import com.lifeos.api.ai.dto.AiAsyncJobResultDTO;
import com.lifeos.api.ai.dto.AiAsyncJobUpdateDTO;
import com.lifeos.api.ai.dto.AiJobStatus;
import com.lifeos.api.ai.dto.AiJobType;
import com.lifeos.api.ai.dto.HandoffSkillResultDTO;
import com.lifeos.api.ai.dto.HandoffSkillSourceDTO;
import com.lifeos.api.ai.mq.AiMqConstants;
import com.lifeos.api.behavior.client.BehaviorFeignClient;
import com.lifeos.api.behavior.dto.BehaviorEventCommand;
import com.lifeos.api.behavior.dto.DashboardStatsDTO;
import com.lifeos.api.behavior.mq.BehaviorMqConstants;
import com.lifeos.common.response.Result;
import com.lifeos.note.domain.entity.AiWorkflowJob;
import com.lifeos.note.domain.entity.HandoffSkill;
import com.lifeos.note.domain.entity.HandoffSkillChat;
import com.lifeos.note.domain.entity.Note;
import com.lifeos.note.mapper.AiWorkflowJobMapper;
import com.lifeos.note.mapper.HandoffSkillChatMapper;
import com.lifeos.note.mapper.HandoffSkillMapper;
import com.lifeos.note.mapper.NoteMapper;
import com.lifeos.note.service.AiJobService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
/**
 * 笔记侧 AI 任务桥接服务。
 *
 * 它在整条 AI 链路中的职责是：
 * 1. 接收笔记服务发起的 AI 请求
 * 2. 先把任务记录写到 ai_workflow_job 表
 * 3. 再通过 RocketMQ 把任务投递给 AI 服务
 * 4. AI 服务完成后，再把结果回写到笔记、任务或行为统计侧
 *
 * 这样拆分后，前端只需要轮询任务状态，不需要等待模型同步返回。
 */
public class AiJobServiceImpl extends ServiceImpl<AiWorkflowJobMapper, AiWorkflowJob> implements AiJobService {

    private static final Set<String> NOTE_JOB_TYPES = Set.of(
            AiJobType.SUMMARY,
            AiJobType.ORGANIZE,
            AiJobType.EXTRACT_TASKS);

    @Resource
    private NoteMapper noteMapper;

    @Resource
    private HandoffSkillMapper handoffSkillMapper;

    @Resource
    private HandoffSkillChatMapper handoffSkillChatMapper;

    @Resource
    private BehaviorFeignClient behaviorFeignClient;

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public AiAsyncJobDTO submitNoteJob(Long userId, Long noteId, String jobType) {
        String normalizedJobType = normalizeJobType(jobType);
        if (!NOTE_JOB_TYPES.contains(normalizedJobType)) {
            throw new RuntimeException("Unsupported note job type");
        }

        Note note = getOwnedNote(userId, noteId);
        AiAsyncJobCommand command = new AiAsyncJobCommand();
        command.setUserId(userId);
        command.setNoteId(noteId);
        command.setJobType(normalizedJobType);
        command.setTitle(note.getTitle());
        command.setContent(note.getContent());
        command.setTags(note.getTags());
        return createAndDispatchJob(command);
    }

    @Override
    public AiAsyncJobDTO submitWeeklyReview(Long userId) {
        AiAsyncJobCommand command = new AiAsyncJobCommand();
        command.setUserId(userId);
        command.setJobType(AiJobType.WEEKLY_REVIEW);
        command.setDashboard(fetchDashboardStats(userId));
        return createAndDispatchJob(command);
    }

    @Override
    public AiAsyncJobDTO submitSkillDistillJob(Long userId, Long skillId, String skillName, String roleDescription,
            List<HandoffSkillSourceDTO> sources) {
        AiAsyncJobCommand command = new AiAsyncJobCommand();
        command.setUserId(userId);
        command.setSkillId(skillId);
        command.setJobType(AiJobType.SKILL_DISTILL);
        command.setSkillName(skillName);
        command.setRoleDescription(roleDescription);
        command.setSkillSources(sources == null ? List.of() : sources);
        return createAndDispatchJob(command);
    }

    @Override
    public AiAsyncJobDTO submitSkillAskJob(Long userId, Long skillId, String skillName, String roleDescription,
            String question, HandoffSkillResultDTO handoffSkill, List<HandoffSkillSourceDTO> sources) {
        AiAsyncJobCommand command = new AiAsyncJobCommand();
        command.setUserId(userId);
        command.setSkillId(skillId);
        command.setJobType(AiJobType.SKILL_ASK);
        command.setSkillName(skillName);
        command.setRoleDescription(roleDescription);
        command.setQuestion(question);
        command.setHandoffSkill(handoffSkill);
        command.setSkillSources(sources == null ? List.of() : sources);
        return createAndDispatchJob(command);
    }

    @Override
    public AiAsyncJobDTO getJob(Long userId, Long jobId) {
        return toDto(getOwnedJob(userId, jobId));
    }

    @Override
    public List<AiAsyncJobDTO> listJobs(Long userId, Long noteId, String jobType, Integer limit) {
        LambdaQueryWrapper<AiWorkflowJob> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiWorkflowJob::getUserId, userId)
                .orderByDesc(AiWorkflowJob::getCreateTime);
        if (noteId != null) {
            wrapper.eq(AiWorkflowJob::getNoteId, noteId);
        }
        if (StringUtils.hasText(jobType)) {
            wrapper.eq(AiWorkflowJob::getJobType, normalizeJobType(jobType));
        }
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + Math.min(limit, 50));
        }
        return this.list(wrapper).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<AiAsyncJobDTO> listSkillJobs(Long userId, Long skillId, Integer limit) {
        LambdaQueryWrapper<AiWorkflowJob> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiWorkflowJob::getUserId, userId)
                .eq(AiWorkflowJob::getSkillId, skillId)
                .orderByDesc(AiWorkflowJob::getCreateTime);
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + Math.min(limit, 50));
        }
        return this.list(wrapper).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public void updateJobStatus(Long jobId, AiAsyncJobUpdateDTO request) {
        if (request == null || !StringUtils.hasText(request.getStatus())) {
            throw new RuntimeException("Job status is required");
        }

        AiWorkflowJob job = baseMapper.selectById(jobId);
        if (job == null) {
            throw new RuntimeException("Job not found");
        }

        String normalizedStatus = request.getStatus().trim().toUpperCase(Locale.ROOT);
        // AI worker 回调可能因为网络重试而重复投递。
        // 一旦任务已经成功，就直接忽略后续重复成功回调，避免重复回写。
        if (AiJobStatus.SUCCESS.equals(job.getStatus())) {
            return;
        }

        LambdaUpdateWrapper<AiWorkflowJob> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiWorkflowJob::getId, jobId)
                .set(AiWorkflowJob::getStatus, normalizedStatus)
                .set(AiWorkflowJob::getErrorMessage, request.getErrorMessage());

        if (AiJobStatus.SUCCESS.equals(normalizedStatus)) {
            wrapper.set(AiWorkflowJob::getResultPayload, JSON.toJSONString(request.getResult()))
                    .set(AiWorkflowJob::getFinishedTime, new Date());
        }
        if (AiJobStatus.FAILED.equals(normalizedStatus)) {
            wrapper.set(AiWorkflowJob::getFinishedTime, new Date());
        }
        this.update(wrapper);

        if (AiJobStatus.SUCCESS.equals(normalizedStatus)) {
            applyJobResult(job, request.getResult());
        } else if (AiJobStatus.FAILED.equals(normalizedStatus)) {
            applyJobFailure(job);
        }
    }

    private AiAsyncJobDTO createAndDispatchJob(AiAsyncJobCommand command) {
        // 先落库再发 MQ，避免出现“消息发出去了，但数据库里没有任务记录”的问题。
        // 这样即使 MQ 发送失败，前端和后台也能看到一条失败任务记录。
        AiWorkflowJob job = new AiWorkflowJob();
        job.setId(IdWorker.getId());
        job.setUserId(command.getUserId());
        job.setNoteId(command.getNoteId());
        job.setSkillId(command.getSkillId());
        job.setJobType(command.getJobType());
        job.setStatus(AiJobStatus.PENDING);
        command.setJobId(job.getId());
        job.setRequestPayload(JSON.toJSONString(command));
        this.save(job);

        try {
            rocketMQTemplate.syncSend(AiMqConstants.TOPIC, JSON.toJSONString(command), AiMqConstants.PRODUCER_TIMEOUT_MS);
            return toDto(job);
        } catch (Exception ex) {
            log.warn("Failed to dispatch ai job {}", job.getId(), ex);
            markJobFailed(job.getId(), "Failed to enqueue AI job");
            throw new RuntimeException("Failed to enqueue AI job");
        }
    }

    private DashboardStatsDTO fetchDashboardStats(Long userId) {
        try {
            Result<DashboardStatsDTO> result = behaviorFeignClient.getDashboardStats(userId);
            if (result != null && result.getCode() == 200 && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception ex) {
            log.warn("Failed to fetch dashboard stats for weekly review", ex);
        }
        return new DashboardStatsDTO();
    }

    private AiAsyncJobDTO toDto(AiWorkflowJob job) {
        AiAsyncJobDTO dto = new AiAsyncJobDTO();
        dto.setId(job.getId());
        dto.setNoteId(job.getNoteId());
        dto.setSkillId(job.getSkillId());
        dto.setNoteTitle(resolveNoteTitle(job));
        dto.setSkillName(resolveSkillName(job));
        dto.setJobType(job.getJobType());
        dto.setStatus(job.getStatus());
        dto.setErrorMessage(job.getErrorMessage());
        dto.setCreateTime(job.getCreateTime());
        dto.setUpdateTime(job.getUpdateTime());
        dto.setFinishedTime(job.getFinishedTime());
        if (StringUtils.hasText(job.getResultPayload())) {
            dto.setResult(JSON.parseObject(job.getResultPayload(), AiAsyncJobResultDTO.class));
        }
        return dto;
    }

    private String resolveNoteTitle(AiWorkflowJob job) {
        if (job.getNoteId() == null || job.getUserId() == null) {
            return null;
        }

        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getId, job.getNoteId())
                .eq(Note::getUserId, job.getUserId());
        Note note = noteMapper.selectOne(wrapper);
        return note == null ? null : note.getTitle();
    }

    private String resolveSkillName(AiWorkflowJob job) {
        if (job.getSkillId() == null || job.getUserId() == null) {
            return null;
        }
        LambdaQueryWrapper<HandoffSkill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HandoffSkill::getId, job.getSkillId())
                .eq(HandoffSkill::getUserId, job.getUserId());
        HandoffSkill skill = handoffSkillMapper.selectOne(wrapper);
        return skill == null ? null : skill.getName();
    }

    private Note getOwnedNote(Long userId, Long noteId) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getId, noteId)
                .eq(Note::getUserId, userId);
        Note note = noteMapper.selectOne(wrapper);
        if (note == null) {
            throw new RuntimeException("Note not found or access denied");
        }
        return note;
    }

    private AiWorkflowJob getOwnedJob(Long userId, Long jobId) {
        LambdaQueryWrapper<AiWorkflowJob> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiWorkflowJob::getId, jobId)
                .eq(AiWorkflowJob::getUserId, userId);
        AiWorkflowJob job = this.getOne(wrapper);
        if (job == null) {
            throw new RuntimeException("Job not found or access denied");
        }
        return job;
    }

    private void applyJobResult(AiWorkflowJob job, AiAsyncJobResultDTO result) {
        if (result == null) {
            return;
        }

        // 不同任务类型的成功结果，会触发不同的后处理逻辑：
        // - SUMMARY：回写 note.summary
        // - ORGANIZE：记录一次整理行为
        // - EXTRACT_TASKS：记录一次任务提取行为，真正任务创建由前端或后续流程决定
        if (AiJobType.SUMMARY.equals(job.getJobType()) && StringUtils.hasText(result.getSummary())) {
            LambdaUpdateWrapper<Note> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Note::getId, job.getNoteId())
                    .eq(Note::getUserId, job.getUserId())
                    .set(Note::getSummary, result.getSummary());
            noteMapper.update(null, wrapper);
            return;
        }

        if (AiJobType.ORGANIZE.equals(job.getJobType())) {
            recordBehaviorEvent(job.getUserId(), "ORGANIZE_NOTE", job.getNoteId());
            return;
        }

        if (AiJobType.EXTRACT_TASKS.equals(job.getJobType())
                && result.getTasks() != null
                && !result.getTasks().isEmpty()) {
            recordBehaviorEvent(job.getUserId(), "EXTRACT_TASK_FROM_NOTE", job.getNoteId());
            return;
        }

        if (AiJobType.SKILL_DISTILL.equals(job.getJobType()) && result.getHandoffSkill() != null) {
            try {
                LambdaUpdateWrapper<HandoffSkill> wrapper = new LambdaUpdateWrapper<>();
                wrapper.eq(HandoffSkill::getId, job.getSkillId())
                        .eq(HandoffSkill::getUserId, job.getUserId())
                        .set(HandoffSkill::getStatus, "DISTILLED")
                        .set(HandoffSkill::getDistillResult, objectMapper.writeValueAsString(result.getHandoffSkill()))
                        .set(HandoffSkill::getLatestJobId, job.getId());
                handoffSkillMapper.update(null, wrapper);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to save handoff skill result", ex);
            }
            return;
        }

        if (AiJobType.SKILL_ASK.equals(job.getJobType()) && StringUtils.hasText(result.getSkillAnswer())) {
            AiAsyncJobCommand command = JSON.parseObject(job.getRequestPayload(), AiAsyncJobCommand.class);
            HandoffSkillChat chat = new HandoffSkillChat();
            chat.setId(IdWorker.getId());
            chat.setUserId(job.getUserId());
            chat.setSkillId(job.getSkillId());
            chat.setJobId(job.getId());
            chat.setQuestion(command == null ? "" : command.getQuestion());
            chat.setAnswer(result.getSkillAnswer());
            chat.setCitations(JSON.toJSONString(result.getCitations()));
            handoffSkillChatMapper.insert(chat);

            LambdaUpdateWrapper<HandoffSkill> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(HandoffSkill::getId, job.getSkillId())
                    .eq(HandoffSkill::getUserId, job.getUserId())
                    .set(HandoffSkill::getLatestJobId, job.getId());
            handoffSkillMapper.update(null, wrapper);
        }
    }

    private void applyJobFailure(AiWorkflowJob job) {
        if (!AiJobType.SKILL_DISTILL.equals(job.getJobType()) || job.getSkillId() == null) {
            return;
        }
        LambdaUpdateWrapper<HandoffSkill> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HandoffSkill::getId, job.getSkillId())
                .eq(HandoffSkill::getUserId, job.getUserId())
                .set(HandoffSkill::getStatus, "FAILED")
                .set(HandoffSkill::getLatestJobId, job.getId());
        handoffSkillMapper.update(null, wrapper);
    }

    private void markJobFailed(Long jobId, String message) {
        LambdaUpdateWrapper<AiWorkflowJob> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AiWorkflowJob::getId, jobId)
                .set(AiWorkflowJob::getStatus, AiJobStatus.FAILED)
                .set(AiWorkflowJob::getErrorMessage, message)
                .set(AiWorkflowJob::getFinishedTime, new Date());
        this.update(wrapper);
    }

    private void recordBehaviorEvent(Long userId, String actionType, Long targetId) {
        if (userId == null || targetId == null) {
            return;
        }
        try {
            BehaviorEventCommand command = new BehaviorEventCommand();
            command.setEventId(UUID.randomUUID().toString());
            command.setUserId(userId);
            command.setActionType(actionType);
            command.setTargetId(targetId);
            rocketMQTemplate.syncSend(BehaviorMqConstants.TOPIC, JSON.toJSONString(command),
                    BehaviorMqConstants.PRODUCER_TIMEOUT_MS);
        } catch (Exception ex) {
            log.warn("Failed to record behavior event {} for ai job", actionType, ex);
        }
    }

    private String normalizeJobType(String jobType) {
        if (!StringUtils.hasText(jobType)) {
            throw new RuntimeException("Job type is required");
        }
        return jobType.trim().toUpperCase(Locale.ROOT);
    }
}
