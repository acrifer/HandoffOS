package com.lifeos.note.repository;

import java.util.List;

/**
 * Custom repository interface for pgvector operations
 */
public interface NoteEmbeddingRepositoryCustom {

    /**
     * Find similar notes using pgvector cosine similarity
     */
    List<Object[]> findSimilarNotes(Long userId, String queryEmbedding, int limit);
}
