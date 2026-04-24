package com.lifeos.ai.knowledge;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Knowledge Entity
 * Represents an entity extracted from handoff skill sources
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityDTO {
    private String entityType;  // PERSON, PROJECT, PROCESS, CONCEPT
    private String entityName;
    private String description;
    private Double confidence;  // 0.0 to 1.0
}
