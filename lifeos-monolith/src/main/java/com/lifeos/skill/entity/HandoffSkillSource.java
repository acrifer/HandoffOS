package com.lifeos.skill.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Raw source content synced from Feishu and indexed into Dify.
 */
@Data
@Entity
@Table(name = "handoff_skill_source")
public class HandoffSkillSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "source_hash", length = 64)
    private String sourceHash;

    @Column(name = "dify_document_id", length = 128)
    private String difyDocumentId;

    @Column(name = "indexing_status", length = 32)
    private String indexingStatus;

    @Column(name = "source_time")
    private LocalDateTime sourceTime;

    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        if (lastSyncTime == null) {
            lastSyncTime = createTime;
        }
    }
}
