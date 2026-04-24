package com.lifeos.ai.knowledge.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Knowledge Relation
 */
@Data
@Entity
@Table(name = "knowledge_relation")
public class KnowledgeRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    @Column(name = "source_entity_id", nullable = false)
    private Long sourceEntityId;

    @Column(name = "target_entity_id", nullable = false)
    private Long targetEntityId;

    @Column(name = "relation_type", nullable = false, length = 50)
    private String relationType;  // RESPONSIBLE_FOR, DEPENDS_ON, PREREQUISITE, RELATED_TO

    @Column(precision = 3, scale = 2)
    private Double confidence = 0.5;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
