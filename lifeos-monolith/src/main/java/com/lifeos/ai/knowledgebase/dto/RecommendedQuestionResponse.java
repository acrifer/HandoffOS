package com.lifeos.ai.knowledgebase.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedQuestionResponse {
    private String question;
    private String category;
}
