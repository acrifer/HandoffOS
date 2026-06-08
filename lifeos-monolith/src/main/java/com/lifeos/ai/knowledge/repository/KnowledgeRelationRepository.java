package com.lifeos.ai.knowledge.repository;

import com.lifeos.ai.knowledge.entity.KnowledgeRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Knowledge Relation Repository
 */
@Repository
public interface KnowledgeRelationRepository extends JpaRepository<KnowledgeRelation, Long> {

    List<KnowledgeRelation> findBySkillId(Long skillId);

    List<KnowledgeRelation> findBySourceEntityId(Long sourceEntityId);

    List<KnowledgeRelation> findByTargetEntityId(Long targetEntityId);

    @Query("SELECT kr FROM KnowledgeRelation kr WHERE kr.skillId = :skillId " +
           "AND (kr.sourceEntityId = :entityId OR kr.targetEntityId = :entityId)")
    List<KnowledgeRelation> findRelationsByEntity(@Param("skillId") Long skillId,
                                                   @Param("entityId") Long entityId);

    long countBySkillId(Long skillId);

    void deleteBySkillId(Long skillId);
}
