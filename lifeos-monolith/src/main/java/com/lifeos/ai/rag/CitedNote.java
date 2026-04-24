package com.lifeos.ai.rag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cited Note in RAG Response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CitedNote {
    private Long noteId;
    private String title;
    private String excerpt;  // Relevant excerpt from the note
    private Double relevanceScore;  // Similarity score
    private String tags;
}
