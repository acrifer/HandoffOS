package com.lifeos.skill.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Team handoff skill controlled by LifeOS and backed by a Dify dataset.
 */
@Data
@Entity
@Table(name = "handoff_skill")
public class HandoffSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "role_description", columnDefinition = "TEXT")
    private String roleDescription;

    @Column(nullable = false, length = 32)
    private String status = "DRAFT";

    @Column(name = "distill_result", columnDefinition = "TEXT")
    private String distillResult;

    @Column(name = "source_count", nullable = false)
    private Integer sourceCount = 0;

    @Column(name = "document_source_count", nullable = false)
    private Integer documentSourceCount = 0;

    @Column(name = "chat_source_count", nullable = false)
    private Integer chatSourceCount = 0;

    @Column(name = "latest_job_id")
    private Long latestJobId;

    @Column(name = "dify_dataset_id", length = 128)
    private String difyDatasetId;

    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime;

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
