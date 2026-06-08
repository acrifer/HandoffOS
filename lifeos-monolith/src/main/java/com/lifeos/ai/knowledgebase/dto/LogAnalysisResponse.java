package com.lifeos.ai.knowledgebase.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LogAnalysisResponse {
    private String summary;
    private List<String> highFrequencyQuestions = new ArrayList<>();
    private List<String> knowledgeGaps = new ArrayList<>();
    private List<String> promptSuggestions = new ArrayList<>();
}
