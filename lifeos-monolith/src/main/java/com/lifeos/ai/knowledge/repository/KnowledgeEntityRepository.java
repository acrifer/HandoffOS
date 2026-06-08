package com.lifeos.ai.knowledge.repository;

import com.lifeos.ai.knowledge.entity.KnowledgeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Knowledge Entity Repository
 */
@Repository
public interface KnowledgeEntityRepository extends JpaRepository<KnowledgeEntity, Long> {

    List<KnowledgeEntity> findBySkillId(Long skillId);

    List<KnowledgeEntity> findBySkillIdAndEntityType(Long skillId, String entityType);

    Optional<KnowledgeEntity> findBySkillIdAndEntityTypeAndEntityName(
            Long skillId, String entityType, String entityName);

    @Query("SELECT ke FROM KnowledgeEntity ke WHERE ke.skillId = :skillId " +
           "AND ke.entityName LIKE %:keyword%")
    List<KnowledgeEntity> searchByKeyword(@Param("skillId") Long skillId,
                                          @Param("keyword") String keyword);

    long countBySkillId(Long skillId);

    void deleteBySkillId(Long skillId);
}
