package com.lifeos.note.dto;

import lombok.Data;

/**
 * Note Create/Update Request DTO
 */
@Data
public class NoteRequest {
    private String title;
    private String content;
    private String tags;
    private Boolean pinned;
}
