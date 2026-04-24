package com.lifeos.ai.knowledge;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Conflict Detection Result
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConflictResult {
    private String conflictType;  // CONTRADICTION, INCONSISTENCY, AMBIGUITY
    private String description;
    private List<String> sources;  // Source IDs or references
    private Double severity;  // 0.0 to 1.0
    private String recommendation;
}
