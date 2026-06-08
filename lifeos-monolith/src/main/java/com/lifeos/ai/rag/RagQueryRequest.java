package com.lifeos.ai.rag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG Query Request
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagQueryRequest {
    private String query;
    private Integer topK = 5;  // Number of similar notes to retrieve
    private Boolean includeContent = true;  // Include full note content in response
}
