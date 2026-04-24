package com.lifeos.note.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Note Embedding Entity for RAG
 */
@Data
@Entity
@Table(name = "note_embedding")
public class NoteEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "note_id", nullable = false, unique = true)
    private Long noteId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(columnDefinition = "vector(1536)")
    private float[] embedding;

    @Column(name = "embedding_model", length = 50)
    private String embeddingModel = "text-embedding-3-small";

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
