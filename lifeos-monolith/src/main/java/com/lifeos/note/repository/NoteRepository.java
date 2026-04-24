package com.lifeos.note.repository;

import com.lifeos.note.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Note Repository
 */
@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    Optional<Note> findByIdAndUserId(Long id, Long userId);

    Page<Note> findByUserIdOrderByPinnedDescUpdateTimeDesc(Long userId, Pageable pageable);

    List<Note> findByUserId(Long userId);

    @Query("SELECT n FROM Note n WHERE n.userId = :userId " +
           "AND (n.title LIKE %:keyword% OR n.content LIKE %:keyword% OR n.tags LIKE %:keyword%) " +
           "ORDER BY n.pinned DESC, n.updateTime DESC")
    Page<Note> searchByKeyword(@Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT n FROM Note n WHERE n.userId = :userId AND n.tags LIKE %:tag% " +
           "ORDER BY n.pinned DESC, n.updateTime DESC")
    Page<Note> findByUserIdAndTag(@Param("userId") Long userId, @Param("tag") String tag, Pageable pageable);

    long countByUserId(Long userId);
}
