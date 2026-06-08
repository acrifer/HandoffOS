package com.lifeos.ai.knowledgebase.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class QaLogResponse {
    private Long id;
    private Long skillId;
    private String question;
    private String answer;
    private List<String> citations = new ArrayList<>();
    private String conversationId;
    private String difyWorkflowRunId;
    private Integer latencyMs;
    private String status;
    private Boolean noAnswer;
    private LocalDateTime createTime;
}
