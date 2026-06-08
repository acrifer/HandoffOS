package com.lifeos.ai.knowledgebase.repository;

import com.lifeos.ai.knowledgebase.entity.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {

    List<KnowledgeChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    List<KnowledgeChunk> findBySkillIdOrderByCreateTimeDesc(Long skillId);

    void deleteByDocumentId(Long documentId);
}
