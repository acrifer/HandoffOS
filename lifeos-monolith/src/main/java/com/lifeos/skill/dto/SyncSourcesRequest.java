package com.lifeos.skill.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class SyncSourcesRequest {
    private List<String> documentRefs = new ArrayList<>();
    private String chatId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer limit = 80;
}
