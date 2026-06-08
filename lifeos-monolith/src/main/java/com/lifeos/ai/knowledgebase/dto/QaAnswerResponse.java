package com.lifeos.ai.knowledgebase.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class QaAnswerResponse {
    private String answer;
    private List<String> citations = new ArrayList<>();
    private Long qaLogId;
    private Long jobId;
    private String conversationId;
    private String difyWorkflowRunId;
    private Boolean noAnswer;
}
