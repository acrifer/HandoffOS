package com.lifeos.admin.domain.vo;

import lombok.Data;

import java.util.Date;

@Data
public class AdminAiJobVO {
    private Long id;
    private Long userId;
    private String username;
    private Long noteId;
    private String noteTitle;
    private String jobType;
    private String status;
    private String errorMessage;
    private Date createTime;
    private Date finishedTime;
}
