package com.lifeos.ai.knowledgebase.repository;

import com.lifeos.ai.knowledgebase.entity.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {

    Optional<PromptTemplate> findFirstByScenarioAndEnabledTrueOrderByVersionDesc(String scenario);
}
