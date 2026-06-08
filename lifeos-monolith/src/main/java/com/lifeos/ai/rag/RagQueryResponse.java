package com.lifeos.ai.rag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG Query Response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagQueryResponse {
    private String answer;
    private List<CitedNote> sources;
    private Integer retrievedCount;
    private Long responseTimeMs;
    private String model;
}
