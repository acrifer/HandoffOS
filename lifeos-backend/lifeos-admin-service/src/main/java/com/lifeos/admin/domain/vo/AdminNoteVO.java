package com.lifeos.admin.domain.vo;

import lombok.Data;

import java.util.Date;

@Data
public class AdminNoteVO {
    private Long id;
    private Long userId;
    private String username;
    private String title;
    private String tags;
    private String summary;
    private Boolean pinned;
    private String reviewState;
    private Date nextReviewAt;
    private Date updateTime;
}
