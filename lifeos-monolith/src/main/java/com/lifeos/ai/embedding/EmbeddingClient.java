package com.lifeos.ai.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Embedding API Client
 * Calls DeepSeek/OpenAI Embedding API to generate vector embeddings
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingClient {

    private final AiProperties aiProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generate embedding vector for text
     * @param text Input text
     * @return float array of embedding vector (1536 dimensions)
     */
    public float[] generateEmbedding(String text) {
        if (!aiProperties.hasApiKey()) {
            log.warn("AI API key not configured, returning mock embedding");
            return generateMockEmbedding(text);
        }

        try {
            String url = aiProperties.getBaseUrl() + "/embeddings";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", aiProperties.getEmbeddingModel());
            requestBody.put("input", text);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(aiProperties.getApiKey());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(url, request, String.class);

            return parseEmbeddingResponse(response);

        } catch (Exception e) {
            log.error("Failed to generate embedding via API: {}", e.getMessage());
            return generateMockEmbedding(text);
        }
    }

    /**
     * Parse embedding from API response
     */
    private float[] parseEmbeddingResponse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode embeddingNode = root.path("data").get(0).path("embedding");

        int dimensions = aiProperties.getEmbeddingDimensions();
        float[] embedding = new float[dimensions];

        for (int i = 0; i < dimensions && i < embeddingNode.size(); i++) {
            embedding[i] = (float) embeddingNode.get(i).asDouble();
        }

        return embedding;
    }

    /**
     * Generate mock embedding for development/testing
     * Uses simple hash-based approach for deterministic results
     */
    private float[] generateMockEmbedding(String text) {
        int dimensions = aiProperties.getEmbeddingDimensions();
        float[] embedding = new float[dimensions];

        // Simple hash-based mock embedding
        int hash = text.hashCode();
        for (int i = 0; i < dimensions; i++) {
            embedding[i] = (float) Math.sin(hash + i) * 0.1f;
        }

        // Normalize to unit vector
        float norm = 0;
        for (float v : embedding) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);

        for (int i = 0; i < dimensions; i++) {
            embedding[i] /= norm;
        }

        return embedding;
    }

    /**
     * Calculate cosine similarity between two embeddings
     */
    public double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Embedding dimensions must match");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
