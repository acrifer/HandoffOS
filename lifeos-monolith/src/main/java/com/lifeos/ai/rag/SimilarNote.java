package com.lifeos.ai.rag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Similar Note Result
 * Represents a note retrieved from vector search
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimilarNote {
    private Long noteId;
    private Long userId;
    private Double similarity;  // Cosine similarity score (0-1)
    private String title;
    private String content;
    private String tags;
}
