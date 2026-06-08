package com.lifeos.ai.knowledgebase.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "ai_feedback")
public class AiFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "qa_log_id", nullable = false)
    private Long qaLogId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer rating;

    @Column(name = "feedback_type", length = 32)
    private String feedbackType;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
