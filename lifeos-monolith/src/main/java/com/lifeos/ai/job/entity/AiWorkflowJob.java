package com.lifeos.ai.job.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Auditable AI workflow job across sync, distill and ask flows.
 */
@Data
@Entity
@Table(name = "ai_workflow_job")
public class AiWorkflowJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "note_id")
    private Long noteId;

    @Column(name = "skill_id")
    private Long skillId;

    @Column(name = "job_type", nullable = false, length = 32)
    private String jobType;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    @Column(name = "result_payload", columnDefinition = "TEXT")
    private String resultPayload;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "dify_workflow_run_id", length = 128)
    private String difyWorkflowRunId;

    @Column(name = "finished_time")
    private LocalDateTime finishedTime;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = createTime;
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
