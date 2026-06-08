package com.lifeos.ai.knowledgebase.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "knowledge_document")
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "dify_document_id", length = 128)
    private String difyDocumentId;

    @Column(nullable = false, length = 32)
    private String status = "UPLOADED";

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "raw_content", columnDefinition = "TEXT")
    private String rawContent;

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
