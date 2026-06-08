package com.lifeos.ai.knowledge.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Knowledge Entity
 */
@Data
@Entity
@Table(name = "knowledge_entity")
public class KnowledgeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;  // PERSON, PROJECT, PROCESS, CONCEPT

    @Column(name = "entity_name", nullable = false)
    private String entityName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private Double confidence = 0.5;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
