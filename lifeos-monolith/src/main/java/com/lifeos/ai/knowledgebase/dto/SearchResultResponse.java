package com.lifeos.ai.knowledgebase.dto;

import lombok.Data;

@Data
public class SearchResultResponse {
    private Long chunkId;
    private Long documentId;
    private String sourceTitle;
    private String sourceLocator;
    private String contentPreview;
    private Double score;
}
