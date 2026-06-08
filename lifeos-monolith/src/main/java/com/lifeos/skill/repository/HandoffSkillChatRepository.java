package com.lifeos.skill.repository;

import com.lifeos.skill.entity.HandoffSkillChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HandoffSkillChatRepository extends JpaRepository<HandoffSkillChat, Long> {

    List<HandoffSkillChat> findBySkillIdOrderByCreateTimeDesc(Long skillId);
}
