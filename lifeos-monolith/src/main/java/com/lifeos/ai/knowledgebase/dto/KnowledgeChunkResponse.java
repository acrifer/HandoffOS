package com.lifeos.ai.knowledgebase.dto;

import lombok.Data;

@Data
public class KnowledgeChunkResponse {
    private Long id;
    private Integer chunkIndex;
    private String contentPreview;
    private String sourceTitle;
    private String sourceLocator;
}
