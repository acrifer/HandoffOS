package com.lifeos.integration.feishu;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeishuSourceService {

    private final FeishuClient feishuClient;
    private final FeishuProperties properties;

    public List<FeishuSourceItem> syncSources(List<String> documentRefs,
                                              String chatId,
                                              LocalDateTime startTime,
                                              LocalDateTime endTime,
                                              Integer requestedLimit) {
        if (!feishuClient.isConfigured()) {
            throw new IllegalStateException("Feishu credentials are not configured");
        }

        boolean hasDocuments = documentRefs != null && documentRefs.stream().anyMatch(ref -> ref != null && !ref.isBlank());
        boolean hasChat = chatId != null && !chatId.isBlank();
        if (!hasDocuments && !hasChat) {
            throw new IllegalArgumentException("No Feishu source provided");
        }

        List<FeishuSourceItem> sources = new java.util.ArrayList<>();
        if (documentRefs != null) {
            for (String documentRef : documentRefs) {
                if (documentRef != null && !documentRef.isBlank()) {
                    sources.add(feishuClient.fetchDocument(documentRef));
                }
            }
        }
        if (hasChat) {
            int limit = requestedLimit != null ? requestedLimit : properties.getChatHistoryLimit();
            sources.addAll(feishuClient.fetchChatMessages(chatId, startTime, endTime, limit));
        }

        return sources;
    }
}
