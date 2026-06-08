package com.lifeos.task.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime deadline;
    private String tags;
    private Long sourceNoteId;
    private Long skillId;
    private Long sourceQaLogId;
    private Short status;
    private LocalDateTime createTime;
    private LocalDateTime completeTime;
}
