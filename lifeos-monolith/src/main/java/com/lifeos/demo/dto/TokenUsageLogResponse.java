package com.lifeos.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TokenUsageLogResponse {
    private Long id;
    private String deviceId;
    private Long userId;
    private Long skillId;
    private String sourceType;
    private String operationType;
    private Long requestTokens;
    private Long responseTokens;
    private Boolean estimated;
    private String externalRunId;
    private String status;
    private LocalDateTime createTime;
}
