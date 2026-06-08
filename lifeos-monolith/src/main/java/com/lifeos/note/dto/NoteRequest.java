package com.lifeos.note.dto;

import lombok.Data;

/**
 * Note Create/Update Request DTO
 */
@Data
public class NoteRequest {
    private Long id;
    private String title;
    private String content;
    private String tags;
    private String summary;
    private Boolean pinned;
    private String reviewState;
    private String nextReviewAt;
    private String lastReviewedAt;
}
