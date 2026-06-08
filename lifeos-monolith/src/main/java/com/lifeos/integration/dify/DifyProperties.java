package com.lifeos.integration.dify;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "dify")
public class DifyProperties {

    private String baseUrl = "http://localhost:5001/v1";
    private String apiKey;
    private String distillWorkflowKey;
    private String askAppKey;
    private Boolean demoMode = false;

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty() && !"mock".equalsIgnoreCase(apiKey.trim());
    }

    public boolean hasDistillWorkflowKey() {
        return distillWorkflowKey != null && !distillWorkflowKey.trim().isEmpty() && !"mock".equalsIgnoreCase(distillWorkflowKey.trim());
    }

    public boolean hasAskAppKey() {
        return askAppKey != null && !askAppKey.trim().isEmpty() && !"mock".equalsIgnoreCase(askAppKey.trim());
    }
}
