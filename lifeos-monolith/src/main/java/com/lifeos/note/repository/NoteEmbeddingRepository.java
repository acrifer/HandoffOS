package com.lifeos.note.repository;

import com.lifeos.note.entity.NoteEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Note Embedding Repository
 */
@Repository
public interface NoteEmbeddingRepository extends JpaRepository<NoteEmbedding, Long> {

    Optional<NoteEmbedding> findByNoteId(Long noteId);

    @Modifying
    @Query("DELETE FROM NoteEmbedding ne WHERE ne.noteId = :noteId")
    void deleteByNoteId(@Param("noteId") Long noteId);

    /**
     * Find similar notes using pgvector cosine similarity
     * Native query for vector operations
     */
    @Query(value = "SELECT ne.note_id, ne.user_id, " +
                   "1 - (ne.embedding <=> CAST(:queryEmbedding AS vector)) as similarity " +
                   "FROM note_embedding ne " +
                   "WHERE ne.user_id = :userId " +
                   "ORDER BY ne.embedding <=> CAST(:queryEmbedding AS vector) " +
                   "LIMIT :limit",
           nativeQuery = true)
    List<Object[]> findSimilarNotes(@Param("userId") Long userId,
                                     @Param("queryEmbedding") String queryEmbedding,
                                     @Param("limit") int limit);

    long countByUserId(Long userId);
}
