package com.lifeos.ai.knowledgebase.dto;

import lombok.Data;

@Data
public class FeedbackRequest {
    private Integer rating;
    private String feedbackType;
    private String comment;
}
