package com.lifeos.skill.repository;

import com.lifeos.skill.entity.HandoffSkillSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HandoffSkillSourceRepository extends JpaRepository<HandoffSkillSource, Long> {

    List<HandoffSkillSource> findBySkillIdOrderByCreateTimeDesc(Long skillId);

    List<HandoffSkillSource> findBySkillId(Long skillId);

    Optional<HandoffSkillSource> findBySkillIdAndContentHash(Long skillId, String contentHash);

    long countBySkillId(Long skillId);

    long countBySkillIdAndSourceType(Long skillId, String sourceType);
}
