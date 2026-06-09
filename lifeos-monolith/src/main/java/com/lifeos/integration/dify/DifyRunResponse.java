package com.lifeos.integration.dify;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DifyRunResponse {
    private String workflowRunId;
    private String taskId;
    private String answer;
    private Map<String, Object> outputs;
    private List<String> citations = new ArrayList<>();
    private Long requestTokens;
    private Long responseTokens;
    private Long totalTokens;
    private boolean usageEstimated = true;
    private boolean demo;
}
