package com.lifeos.admin.domain.dto;

import lombok.Data;

import java.util.Date;

@Data
public class AdminNoteReviewStateDTO {
    private String reviewState;
    private Date nextReviewAt;
}
