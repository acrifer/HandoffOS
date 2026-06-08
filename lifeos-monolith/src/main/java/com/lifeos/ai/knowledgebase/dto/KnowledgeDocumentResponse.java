package com.lifeos.ai.knowledgebase.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class KnowledgeDocumentResponse {
    private Long id;
    private Long skillId;
    private String title;
    private String sourceType;
    private String sourceUrl;
    private String difyDocumentId;
    private String status;
    private String summary;
    private Integer chunkCount = 0;
    private List<KnowledgeChunkResponse> chunks = new ArrayList<>();
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
