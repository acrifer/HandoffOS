package com.lifeos.integration.dify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Thin Dify API client. It keeps LifeOS as the control plane while Dify hosts
 * knowledge indexing and workflow/chat execution.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DifyClient {

    private final DifyProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public DifyDatasetResponse ensureDataset(String currentDatasetId, String skillName, String description) {
        requireApiKey(properties.getApiKey(), "Dify API key is not configured");
        requireRealMode();
        if (currentDatasetId != null && !currentDatasetId.isBlank()) {
            if (datasetExists(currentDatasetId)) {
                return new DifyDatasetResponse(currentDatasetId, skillName, false);
            }
            log.warn("Dify dataset {} is no longer available, recreating for skill {}", currentDatasetId, skillName);
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("name", skillName);
            body.put("description", description == null ? "" : description);
            body.put("indexing_technique", "high_quality");
            body.put("permission", "only_me");

            String response = restTemplate.postForObject(
                    apiUrl("/datasets"),
                    new HttpEntity<>(body, headers(properties.getApiKey())),
                    String.class
            );
            JsonNode root = objectMapper.readTree(response);
            String datasetId = root.path("id").asText();
            if (datasetId == null || datasetId.isBlank()) {
                throw new IllegalStateException("Dify dataset creation returned empty dataset id");
            }
            return new DifyDatasetResponse(datasetId, root.path("name").asText(skillName), false);
        } catch (Exception e) {
            throw wrap("Failed to create Dify dataset", e);
        }
    }

    private boolean datasetExists(String datasetId) {
        try {
            restTemplate.exchange(
                    apiUrl("/datasets/" + datasetId),
                    HttpMethod.GET,
                    new HttpEntity<>(headers(properties.getApiKey())),
                    String.class
            );
            return true;
        } catch (RestClientResponseException e) {
            if (e.getRawStatusCode() == 404) {
                return false;
            }
            throw new IllegalStateException("Dify dataset check failed for " + datasetId + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw wrap("Dify dataset check failed for " + datasetId, e);
        }
    }

    public DifyDocumentResponse upsertDocument(String datasetId, String title, String content) {
        requireApiKey(properties.getApiKey(), "Dify API key is not configured");
        requireRealMode();
        if (datasetId == null || datasetId.isBlank()) {
            throw new IllegalArgumentException("Dify dataset id is required");
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("name", title);
            body.put("text", content);
            body.put("indexing_technique", "high_quality");
            body.put("process_rule", Map.of("mode", "automatic"));

            String response = restTemplate.postForObject(
                    apiUrl("/datasets/" + datasetId + "/document/create-by-text"),
                    new HttpEntity<>(body, headers(properties.getApiKey())),
                    String.class
            );
            JsonNode root = objectMapper.readTree(response);
            JsonNode document = root.has("document") ? root.path("document") : root;
            String documentId = document.path("id").asText();
            String indexingStatus = document.path("indexing_status").asText("indexing");
            if (documentId == null || documentId.isBlank()) {
                throw new IllegalStateException("Dify document upsert returned empty document id");
            }
            return new DifyDocumentResponse(documentId, indexingStatus, false);
        } catch (Exception e) {
            throw wrap("Failed to upsert Dify document", e);
        }
    }

    public DifyRunResponse runDistillWorkflow(String skillName,
                                              String roleDescription,
                                              String datasetId,
                                              List<Map<String, Object>> sources,
                                              String userKey) {
        requireApiKey(properties.getApiKey(), "Dify API key is not configured");
        requireApiKey(properties.getDistillWorkflowKey(), "Dify distill workflow key is not configured");
        requireRealMode();
        if (datasetId == null || datasetId.isBlank()) {
            throw new IllegalArgumentException("Dify dataset id is required");
        }

        try {
            Map<String, Object> inputs = new LinkedHashMap<>();
            inputs.put("skill_name", skillName);
            inputs.put("role_description", roleDescription == null ? "" : roleDescription);
            inputs.put("dataset_id", datasetId);
            String sourcesJson = objectMapper.writeValueAsString(sources == null ? List.of() : sources);
            inputs.put("sources", sourcesJson);
            inputs.put("sources_json", sourcesJson);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("inputs", inputs);
            body.put("response_mode", "blocking");
            body.put("user", userKey);

            String response = restTemplate.postForObject(
                    apiUrl("/workflows/run"),
                    new HttpEntity<>(body, headers(properties.getDistillWorkflowKey())),
                    String.class
            );
            return parseWorkflowResponse(response, false);
        } catch (Exception e) {
            throw wrap("Failed to run Dify distill workflow", e);
        }
    }

    public DifyRunResponse askSkill(String skillName,
                                    String datasetId,
                                    String question,
                                    String userKey) {
        requireApiKey(properties.getApiKey(), "Dify API key is not configured");
        requireApiKey(properties.getAskAppKey(), "Dify ask app key is not configured");
        requireRealMode();
        if (datasetId == null || datasetId.isBlank()) {
            throw new IllegalArgumentException("Dify dataset id is required");
        }

        try {
            RetrievedContext retrieval = retrieveContext(datasetId, question);
            Map<String, Object> inputs = new LinkedHashMap<>();
            inputs.put("skill_name", skillName);
            inputs.put("dataset_id", datasetId);
            inputs.put("retrieved_context", retrieval.context());
            inputs.put("citations", String.join("\n", retrieval.citations()));

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("inputs", inputs);
            body.put("query", question);
            body.put("response_mode", "blocking");
            body.put("user", userKey);

            String response = restTemplate.postForObject(
                    apiUrl("/chat-messages"),
                    new HttpEntity<>(body, headers(properties.getAskAppKey())),
                    String.class
            );
            DifyRunResponse run = parseChatResponse(response, false);
            if ((run.getCitations() == null || run.getCitations().isEmpty()) && !retrieval.citations().isEmpty()) {
                run.setCitations(retrieval.citations());
            }
            return run;
        } catch (Exception e) {
            throw wrap("Failed to ask Dify skill", e);
        }
    }

    private RetrievedContext retrieveContext(String datasetId, String question) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", question);
        body.put("external_retrieval_model", Map.of(
                "top_k", 5,
                "score_threshold_enabled", false
        ));

        String response = restTemplate.postForObject(
                apiUrl("/datasets/" + datasetId + "/retrieve"),
                new HttpEntity<>(body, headers(properties.getApiKey())),
                String.class
        );
        JsonNode root = objectMapper.readTree(response);
        JsonNode records = root.path("records");
        if (!records.isArray()) {
            return new RetrievedContext("", List.of());
        }

        List<String> citations = new ArrayList<>();
        StringBuilder context = new StringBuilder();
        int index = 1;
        for (JsonNode record : records) {
            JsonNode segment = record.path("segment");
            String content = segment.path("content").asText("");
            if (content.isBlank()) {
                continue;
            }
            String source = firstText(segment, "document_name", "document_id");
            JsonNode document = segment.path("document");
            if (source.isBlank() && !document.isMissingNode()) {
                source = firstText(document, "name", "id");
            }
            if (source.isBlank()) {
                source = "Dify segment " + segment.path("id").asText(String.valueOf(index));
            }
            citations.add(source);
            context.append("[")
                    .append(index)
                    .append("] ")
                    .append(source)
                    .append("\n")
                    .append(content)
                    .append("\n\n");
            index++;
        }
        return new RetrievedContext(context.toString().trim(), citations);
    }

    private DifyRunResponse parseWorkflowResponse(String response, boolean compatibilityDemoFlag) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode data = root.has("data") ? root.path("data") : root;
        JsonNode outputs = data.has("outputs") ? data.path("outputs") : root.path("outputs");

        Map<String, Object> outputMap = outputs.isMissingNode() || outputs.isNull()
                ? new LinkedHashMap<>()
                : objectMapper.convertValue(outputs, Map.class);
        String answer = firstText(outputs, "answer", "result", "text", "output");
        DifyRunResponse run = new DifyRunResponse();
        run.setWorkflowRunId(data.path("workflow_run_id").asText(root.path("workflow_run_id").asText(null)));
        run.setTaskId(data.path("task_id").asText(root.path("task_id").asText(null)));
        run.setAnswer(answer);
        run.setOutputs(outputMap);
        run.setCitations(extractCitations(root));
        applyUsage(run, root);
        run.setDemo(compatibilityDemoFlag);
        if ((run.getAnswer() == null || run.getAnswer().isBlank()) && run.getOutputs().isEmpty()) {
            throw new IllegalStateException("Dify workflow returned empty outputs");
        }
        return run;
    }

    private DifyRunResponse parseChatResponse(String response, boolean compatibilityDemoFlag) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        DifyRunResponse run = new DifyRunResponse();
        run.setWorkflowRunId(root.path("workflow_run_id").asText(root.path("message_id").asText(null)));
        run.setTaskId(root.path("task_id").asText(null));
        run.setAnswer(root.path("answer").asText(firstText(root.path("data"), "answer", "result")));
        run.setOutputs(Map.of("answer", run.getAnswer()));
        run.setCitations(extractCitations(root));
        applyUsage(run, root);
        run.setDemo(compatibilityDemoFlag);
        if (run.getAnswer() == null || run.getAnswer().isBlank()) {
            throw new IllegalStateException("Dify chat response returned empty answer");
        }
        return run;
    }

    private List<String> extractCitations(JsonNode root) {
        List<String> citations = new ArrayList<>();
        JsonNode resources = root.path("metadata").path("retriever_resources");
        if (resources.isArray()) {
            for (JsonNode item : resources) {
                String name = item.path("document_name").asText(item.path("source").asText(""));
                if (!name.isBlank()) {
                    citations.add(name);
                }
            }
        }
        return citations;
    }

    private void applyUsage(DifyRunResponse run, JsonNode root) {
        JsonNode usage = root.path("metadata").path("usage");
        if (usage.isMissingNode() || usage.isNull()) {
            usage = root.path("usage");
        }
        if (usage.isMissingNode() || usage.isNull()) {
            usage = root.path("data").path("usage");
        }
        long promptTokens = firstLong(usage, "prompt_tokens", "input_tokens", "promptTokens", "inputTokens");
        long completionTokens = firstLong(usage, "completion_tokens", "output_tokens", "completionTokens", "outputTokens");
        long totalTokens = firstLong(usage, "total_tokens", "totalTokens");
        if (totalTokens == 0L && (promptTokens > 0L || completionTokens > 0L)) {
            totalTokens = promptTokens + completionTokens;
        }
        if (totalTokens > 0L) {
            run.setRequestTokens(promptTokens);
            run.setResponseTokens(completionTokens);
            run.setTotalTokens(totalTokens);
            run.setUsageEstimated(false);
        }
    }

    private long firstLong(JsonNode node, String... names) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return 0L;
        }
        for (String name : names) {
            JsonNode value = node.path(name);
            if (value.isNumber()) {
                return value.asLong();
            }
        }
        return 0L;
    }

    private HttpHeaders headers(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    private String firstText(JsonNode node, String... names) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        for (String name : names) {
            JsonNode value = node.path(name);
            if (value.isTextual() && !value.asText().isBlank()) {
                return value.asText();
            }
        }
        return "";
    }

    private String apiUrl(String path) {
        return properties.getBaseUrl().replaceAll("/$", "") + path;
    }

    private void requireApiKey(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(message);
        }
    }

    private void requireRealMode() {
        if (Boolean.TRUE.equals(properties.getDemoMode())) {
            throw new IllegalStateException("Dify demo mode is no longer supported for runtime requests");
        }
    }

    private IllegalStateException wrap(String prefix, Exception e) {
        if (e instanceof IllegalStateException state) {
            return state;
        }
        return new IllegalStateException(prefix + ": " + e.getMessage(), e);
    }

    private record RetrievedContext(String context, List<String> citations) {
    }
}
