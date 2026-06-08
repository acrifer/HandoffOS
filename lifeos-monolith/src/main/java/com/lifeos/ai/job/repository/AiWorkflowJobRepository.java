package com.lifeos.ai.job.repository;

import com.lifeos.ai.job.entity.AiWorkflowJob;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiWorkflowJobRepository extends JpaRepository<AiWorkflowJob, Long> {

    Optional<AiWorkflowJob> findByIdAndUserId(Long id, Long userId);

    List<AiWorkflowJob> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    List<AiWorkflowJob> findByUserIdAndSkillIdOrderByCreateTimeDesc(Long userId, Long skillId, Pageable pageable);

    List<AiWorkflowJob> findByUserIdAndJobTypeOrderByCreateTimeDesc(Long userId, String jobType, Pageable pageable);
}
