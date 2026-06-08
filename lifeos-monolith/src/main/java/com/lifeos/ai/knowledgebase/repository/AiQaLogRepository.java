package com.lifeos.ai.knowledgebase.repository;

import com.lifeos.ai.knowledgebase.entity.AiQaLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AiQaLogRepository extends JpaRepository<AiQaLog, Long> {

    Optional<AiQaLog> findByIdAndUserId(Long id, Long userId);

    List<AiQaLog> findByUserIdAndSkillIdOrderByCreateTimeDesc(Long userId, Long skillId, Pageable pageable);

    List<AiQaLog> findBySkillIdOrderByCreateTimeDesc(Long skillId, Pageable pageable);

    List<AiQaLog> findByCreateTimeBetweenOrderByCreateTimeDesc(LocalDateTime start, LocalDateTime end);

    long countByCreateTimeBetween(LocalDateTime start, LocalDateTime end);

    long countByStatusAndCreateTimeBetween(String status, LocalDateTime start, LocalDateTime end);

    long countByNoAnswerTrueAndCreateTimeBetween(LocalDateTime start, LocalDateTime end);
}
