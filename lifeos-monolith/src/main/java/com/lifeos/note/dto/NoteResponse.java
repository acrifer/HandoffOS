package com.lifeos.note.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Note Response DTO
 */
@Data
public class NoteResponse {
    private Long id;
    private String title;
    private String content;
    private String tags;
    private String summary;
    private Boolean pinned;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
