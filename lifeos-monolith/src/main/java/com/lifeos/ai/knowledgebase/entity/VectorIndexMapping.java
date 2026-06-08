package com.lifeos.ai.knowledgebase.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "vector_index_mapping")
public class VectorIndexMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chunk_id", nullable = false)
    private Long chunkId;

    @Column(name = "dify_dataset_id", length = 128)
    private String difyDatasetId;

    @Column(name = "dify_document_id", length = 128)
    private String difyDocumentId;

    @Column(name = "embedding_model", length = 80)
    private String embeddingModel;

    @Column(name = "index_status", length = 32)
    private String indexStatus = "PENDING";

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
