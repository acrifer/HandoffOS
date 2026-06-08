package com.lifeos.ai.knowledgebase.repository;

import com.lifeos.ai.knowledgebase.entity.AiFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AiFeedbackRepository extends JpaRepository<AiFeedback, Long> {

    List<AiFeedback> findByQaLogIdOrderByCreateTimeDesc(Long qaLogId);

    List<AiFeedback> findByCreateTimeBetweenOrderByCreateTimeDesc(LocalDateTime start, LocalDateTime end);

    long countByRatingLessThanEqualAndCreateTimeBetween(Integer rating, LocalDateTime start, LocalDateTime end);
}
