package com.lifeos.ai.knowledgebase.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LogAnalysisRequest {
    private LocalDateTime start;
    private LocalDateTime end;
    private Long skillId;
}
