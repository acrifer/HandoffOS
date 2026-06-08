package com.lifeos.ai.knowledgebase.repository;

import com.lifeos.ai.knowledgebase.entity.VectorIndexMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VectorIndexMappingRepository extends JpaRepository<VectorIndexMapping, Long> {

    List<VectorIndexMapping> findByDifyDocumentId(String difyDocumentId);
}
