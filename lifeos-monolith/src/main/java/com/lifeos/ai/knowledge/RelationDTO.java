package com.lifeos.ai.knowledge;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Knowledge Relation
 * Represents a relation between two entities
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelationDTO {
    private Long sourceEntityId;
    private String sourceEntityName;
    private Long targetEntityId;
    private String targetEntityName;
    private String relationType;  // RESPONSIBLE_FOR, DEPENDS_ON, PREREQUISITE, RELATED_TO
    private Double confidence;
}
