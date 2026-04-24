package com.lifeos.ai.insight;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Learning Pattern Analysis Result
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearningPattern {
    private String patternType;  // TIME_DISTRIBUTION, DEPTH_VS_FRAGMENT, TOPIC_FOCUS
    private String description;
    private List<String> insights;
    private Double score;  // 0.0 to 1.0
    private String recommendation;
}
