package com.lifeos.ai.knowledgebase.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ai_qa_log")
public class AiQaLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Column(columnDefinition = "TEXT")
    private String citations;

    @Column(name = "conversation_id", length = 128)
    private String conversationId;

    @Column(name = "dify_workflow_run_id", length = 128)
    private String difyWorkflowRunId;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(nullable = false, length = 32)
    private String status = "SUCCESS";

    @Column(name = "no_answer", nullable = false)
    private Boolean noAnswer = false;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
