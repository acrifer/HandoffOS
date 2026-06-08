package com.lifeos.ai.job.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.ai.job.dto.AiJobResponse;
import com.lifeos.ai.job.entity.AiWorkflowJob;
import com.lifeos.ai.job.repository.AiWorkflowJobRepository;
import com.lifeos.note.repository.NoteRepository;
import com.lifeos.skill.entity.HandoffSkill;
import com.lifeos.skill.repository.HandoffSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiWorkflowJobService {

    private final AiWorkflowJobRepository jobRepository;
    private final HandoffSkillRepository skillRepository;
    private final NoteRepository noteRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiWorkflowJob startJob(Long userId, Long skillId, String jobType, Object requestPayload) {
        AiWorkflowJob job = new AiWorkflowJob();
        job.setUserId(userId);
        job.setSkillId(skillId);
        job.setJobType(jobType);
        job.setStatus("PROCESSING");
        job.setRequestPayload(toJson(requestPayload));
        return jobRepository.save(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiWorkflowJob markSuccess(AiWorkflowJob job, Object resultPayload, String difyWorkflowRunId) {
        job.setStatus("SUCCESS");
        job.setResultPayload(toJson(resultPayload));
        job.setDifyWorkflowRunId(difyWorkflowRunId);
        job.setFinishedTime(LocalDateTime.now());
        return jobRepository.save(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiWorkflowJob markFailed(AiWorkflowJob job, Exception error) {
        job.setStatus("FAILED");
        job.setErrorMessage(error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
        job.setFinishedTime(LocalDateTime.now());
        return jobRepository.save(job);
    }

    public AiJobResponse getJob(Long userId, Long jobId) {
        return toResponse(jobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new RuntimeException("AI job not found")));
    }

    public List<AiJobResponse> listJobs(Long userId, Long skillId, String jobType, int limit) {
        PageRequest page = PageRequest.of(0, Math.max(1, Math.min(limit, 100)));
        List<AiWorkflowJob> jobs;
        if (skillId != null) {
            jobs = jobRepository.findByUserIdAndSkillIdOrderByCreateTimeDesc(userId, skillId, page);
        } else if (jobType != null && !jobType.isBlank()) {
            jobs = jobRepository.findByUserIdAndJobTypeOrderByCreateTimeDesc(userId, jobType, page);
        } else {
            jobs = jobRepository.findByUserIdOrderByCreateTimeDesc(userId, page);
        }
        return jobs.stream().map(this::toResponse).toList();
    }

    public AiJobResponse toResponse(AiWorkflowJob job) {
        AiJobResponse response = new AiJobResponse();
        response.setId(job.getId());
        response.setUserId(job.getUserId());
        response.setNoteId(job.getNoteId());
        response.setSkillId(job.getSkillId());
        response.setJobType(job.getJobType());
        response.setStatus(job.getStatus());
        response.setRequest(parseJson(job.getRequestPayload()));
        response.setResult(parseJson(job.getResultPayload()));
        response.setErrorMessage(job.getErrorMessage());
        response.setDifyWorkflowRunId(job.getDifyWorkflowRunId());
        response.setFinishedTime(job.getFinishedTime());
        response.setCreateTime(job.getCreateTime());

        if (job.getSkillId() != null) {
            skillRepository.findById(job.getSkillId()).map(HandoffSkill::getName).ifPresent(response::setSkillName);
        }
        if (job.getNoteId() != null) {
            noteRepository.findById(job.getNoteId()).ifPresent(note -> response.setNoteTitle(note.getTitle()));
        }
        return response;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of("raw", json);
        }
    }
}
