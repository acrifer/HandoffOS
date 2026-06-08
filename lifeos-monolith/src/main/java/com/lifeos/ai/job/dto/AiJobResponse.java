package com.lifeos.ai.job.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class AiJobResponse {
    private Long id;
    private Long userId;
    private Long noteId;
    private Long skillId;
    private String noteTitle;
    private String skillName;
    private String jobType;
    private String status;
    private Map<String, Object> request;
    private Map<String, Object> result;
    private String errorMessage;
    private String difyWorkflowRunId;
    private LocalDateTime finishedTime;
    private LocalDateTime createTime;
}
