package com.lifeos.ai.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.config.AiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG Query Service
 * Core service for Retrieval-Augmented Generation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagQueryService {

    private final VectorRepository vectorRepository;
    private final AiProperties aiProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Execute RAG query: retrieve similar notes and generate answer
     */
    public RagQueryResponse query(Long userId, RagQueryRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // Step 1: Retrieve similar notes using vector search
            log.info("RAG Query - Step 1: Retrieving similar notes for user {}", userId);
            List<SimilarNote> similarNotes = vectorRepository.searchSimilarNotes(
                    userId,
                    request.getQuery(),
                    request.getTopK()
            );

            if (similarNotes.isEmpty()) {
                return buildEmptyResponse(startTime);
            }

            // Step 2: Build context from retrieved notes
            log.info("RAG Query - Step 2: Building context from {} notes", similarNotes.size());
            String context = buildContext(similarNotes);

            // Step 3: Generate answer using LLM
            log.info("RAG Query - Step 3: Generating answer with LLM");
            String answer = generateAnswer(request.getQuery(), context);

            // Step 4: Build response with citations
            List<CitedNote> citations = similarNotes.stream()
                    .map(this::toCitedNote)
                    .collect(Collectors.toList());

            long responseTime = System.currentTimeMillis() - startTime;

            RagQueryResponse response = new RagQueryResponse();
            response.setAnswer(answer);
            response.setSources(citations);
            response.setRetrievedCount(similarNotes.size());
            response.setResponseTimeMs(responseTime);
            response.setModel(aiProperties.getModel());

            log.info("RAG Query completed in {}ms", responseTime);
            return response;

        } catch (Exception e) {
            log.error("RAG Query failed: {}", e.getMessage(), e);
            throw new IllegalStateException("RAG Query failed: " + e.getMessage(), e);
        }
    }

    /**
     * Build context string from retrieved notes
     */
    private String buildContext(List<SimilarNote> notes) {
        StringBuilder context = new StringBuilder();

        for (int i = 0; i < notes.size(); i++) {
            SimilarNote note = notes.get(i);
            context.append(String.format("【笔记 %d】%s\n", i + 1, note.getTitle()));

            if (note.getTags() != null && !note.getTags().isEmpty()) {
                context.append(String.format("标签: %s\n", note.getTags()));
            }

            // Truncate content if too long
            String content = note.getContent();
            if (content != null) {
                if (content.length() > 1000) {
                    content = content.substring(0, 1000) + "...";
                }
                context.append(content).append("\n\n");
            }
        }

        return context.toString();
    }

    /**
     * Generate answer using LLM with retrieved context
     */
    private String generateAnswer(String query, String context) {
        if (!aiProperties.hasApiKey()) {
            throw new IllegalStateException("AI API key is not configured");
        }

        try {
            String url = aiProperties.getBaseUrl() + "/chat/completions";

            String systemPrompt = "你是一个智能笔记助手。基于用户的历史笔记回答问题。" +
                    "如果笔记中没有相关信息，请明确告知用户。" +
                    "回答要简洁、准确，并引用具体的笔记内容。";

            String userPrompt = String.format(
                    "用户问题：%s\n\n相关笔记：\n%s\n\n请基于以上笔记回答用户的问题。",
                    query, context
            );

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", aiProperties.getModel());
            requestBody.put("messages", Arrays.asList(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 1000);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(aiProperties.getApiKey());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(url, request, String.class);

            return parseAnswerFromResponse(response);

        } catch (Exception e) {
            log.error("Failed to generate answer via LLM: {}", e.getMessage());
            throw new IllegalStateException("Failed to generate answer via LLM: " + e.getMessage(), e);
        }
    }

    /**
     * Parse answer from LLM API response
     */
    private String parseAnswerFromResponse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        return root.path("choices").get(0).path("message").path("content").asText();
    }

    /**
     * Convert SimilarNote to CitedNote
     */
    private CitedNote toCitedNote(SimilarNote note) {
        String excerpt = note.getContent();
        if (excerpt != null && excerpt.length() > 200) {
            excerpt = excerpt.substring(0, 200) + "...";
        }

        return new CitedNote(
                note.getNoteId(),
                note.getTitle(),
                excerpt,
                note.getSimilarity(),
                note.getTags()
        );
    }

    /**
     * Build empty response when no similar notes found
     */
    private RagQueryResponse buildEmptyResponse(long startTime) {
        RagQueryResponse response = new RagQueryResponse();
        response.setAnswer("抱歉，我在您的笔记中没有找到相关内容。请尝试换个问法，或者先创建一些笔记。");
        response.setSources(new ArrayList<>());
        response.setRetrievedCount(0);
        response.setResponseTimeMs(System.currentTimeMillis() - startTime);
        response.setModel(aiProperties.getModel());
        return response;
    }

}
