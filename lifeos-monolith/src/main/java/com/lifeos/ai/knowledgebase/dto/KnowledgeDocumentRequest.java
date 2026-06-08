package com.lifeos.ai.knowledgebase.dto;

import lombok.Data;

@Data
public class KnowledgeDocumentRequest {
    private String title;
    private String sourceType;
    private String sourceUrl;
    private String content;
}
