package com.lifeos.ai.knowledgebase.repository;

import com.lifeos.ai.knowledgebase.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    List<KnowledgeDocument> findBySkillIdOrderByCreateTimeDesc(Long skillId);

    Optional<KnowledgeDocument> findByIdAndSkillId(Long id, Long skillId);
}
