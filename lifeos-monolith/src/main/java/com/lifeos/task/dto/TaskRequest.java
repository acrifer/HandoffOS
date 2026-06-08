package com.lifeos.task.dto;

import lombok.Data;

@Data
public class TaskRequest {
    private Long id;
    private String title;
    private String description;
    private String deadline;
    private String tags;
    private Long sourceNoteId;
    private Long skillId;
    private Long sourceQaLogId;
}
