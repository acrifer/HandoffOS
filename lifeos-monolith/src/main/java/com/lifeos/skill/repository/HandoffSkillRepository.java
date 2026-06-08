package com.lifeos.skill.repository;

import com.lifeos.skill.entity.HandoffSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HandoffSkillRepository extends JpaRepository<HandoffSkill, Long> {

    List<HandoffSkill> findByUserIdOrderByUpdateTimeDesc(Long userId);

    Optional<HandoffSkill> findByIdAndUserId(Long id, Long userId);
}
