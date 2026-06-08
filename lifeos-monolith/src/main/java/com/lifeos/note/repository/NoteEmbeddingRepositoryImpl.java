package com.lifeos.note.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Custom repository implementation for pgvector operations
 * Uses EntityManager to bypass Spring Data JPA query validation
 */
@Repository
public class NoteEmbeddingRepositoryImpl implements NoteEmbeddingRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Object[]> findSimilarNotes(Long userId, String queryEmbedding, int limit) {
        String sql = "SELECT ne.note_id, ne.user_id, " +
                     "1 - (ne.embedding <=> CAST(:queryEmbedding AS vector)) as similarity " +
                     "FROM note_embedding ne " +
                     "WHERE ne.user_id = :userId " +
                     "ORDER BY ne.embedding <=> CAST(:queryEmbedding AS vector) " +
                     "LIMIT :limit";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        query.setParameter("queryEmbedding", queryEmbedding);
        query.setParameter("limit", limit);

        return query.getResultList();
    }
}
