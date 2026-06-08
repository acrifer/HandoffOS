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
public interface NoteEmbeddingRepository extends JpaRepository<NoteEmbedding, Long>, NoteEmbeddingRepositoryCustom {

    Optional<NoteEmbedding> findByNoteId(Long noteId);

    @Modifying
    @Query("DELETE FROM NoteEmbedding ne WHERE ne.noteId = :noteId")
    void deleteByNoteId(@Param("noteId") Long noteId);

    long countByUserId(Long userId);
}
