package com.lifeos.ai.insight;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Topic Cluster
 * Represents a cluster of notes on similar topics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicCluster {
    private String topicName;
    private List<Long> noteIds;
    private Integer noteCount;
    private Double percentage;  // Percentage of total notes
    private List<String> keywords;
    private String trend;  // INCREASING, STABLE, DECREASING
}
