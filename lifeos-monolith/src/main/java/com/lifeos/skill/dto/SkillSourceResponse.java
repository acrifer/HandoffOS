package com.lifeos.skill.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SkillSourceResponse {
    private Long id;
    private String sourceType;
    private String externalId;
    private String title;
    private String contentPreview;
    private String difyDocumentId;
    private String indexingStatus;
    private LocalDateTime sourceTime;
    private LocalDateTime createTime;
}
