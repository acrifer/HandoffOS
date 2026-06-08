package com.lifeos.skill.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class SkillChatResponse {
    private Long id;
    private Long jobId;
    private String question;
    private String answer;
    private List<String> citations = new ArrayList<>();
    private String difyWorkflowRunId;
    private LocalDateTime createTime;
}
