package com.lifeos.ai.insight;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Insight Report
 * Comprehensive analysis of user's learning patterns
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsightReport {
    private String period;  // e.g., "本周", "本月"
    private Map<String, Integer> statistics;  // Basic stats
    private List<TopicCluster> topicClusters;  // Topic clustering results
    private List<LearningPattern> patterns;  // Learning patterns
    private List<String> recommendations;  // Personalized recommendations
    private Double overallScore;  // Overall learning score (0-1)
    private String summary;  // AI-generated summary
}
