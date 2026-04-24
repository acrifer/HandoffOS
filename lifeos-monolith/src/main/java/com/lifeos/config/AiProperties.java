package com.lifeos.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI Service Configuration Properties
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private String baseUrl;
    private String apiKey;
    private String model;
    private String embeddingModel;
    private Integer embeddingDimensions = 1536;

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
