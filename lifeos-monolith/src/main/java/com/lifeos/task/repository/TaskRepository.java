package com.lifeos.task.repository;

import com.lifeos.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUserIdOrderByStatusAscCreateTimeDesc(Long userId);
    List<Task> findByUserIdAndSkillIdOrderByStatusAscCreateTimeDesc(Long userId, Long skillId);
    Optional<Task> findByIdAndUserId(Long id, Long userId);
}
