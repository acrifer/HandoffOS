package com.lifeos.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ai_token_usage_log")
public class AiTokenUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, length = 120)
    private String deviceId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "skill_id")
    private Long skillId;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(name = "operation_type", nullable = false, length = 50)
    private String operationType;

    @Column(name = "request_tokens", nullable = false)
    private Long requestTokens = 0L;

    @Column(name = "response_tokens", nullable = false)
    private Long responseTokens = 0L;

    @Column(nullable = false)
    private Boolean estimated = true;

    @Column(name = "external_run_id", length = 120)
    private String externalRunId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
