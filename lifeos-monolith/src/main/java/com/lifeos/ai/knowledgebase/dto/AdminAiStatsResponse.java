package com.lifeos.ai.knowledgebase.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class AdminAiStatsResponse {
    private Long usage;
    private Long failedCount;
    private Long noAnswerCount;
    private Long negativeFeedbackCount;
    private Double noAnswerRate;
    private List<String> topQuestions = new ArrayList<>();
    private Long botEventCount;
    private Long botFailedEventCount;
    private Long botBindingCount;
    private Map<String, Long> botCommandDistribution = new LinkedHashMap<>();
}
