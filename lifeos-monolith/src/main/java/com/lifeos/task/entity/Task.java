package com.lifeos.task.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime deadline;

    @Column(length = 255)
    private String tags;

    @Column(name = "source_note_id")
    private Long sourceNoteId;

    @Column(name = "skill_id")
    private Long skillId;

    @Column(name = "source_qa_log_id")
    private Long sourceQaLogId;

    @Column(nullable = false)
    private Short status = 0;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "complete_time")
    private LocalDateTime completeTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }
}
