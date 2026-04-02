package com.lifeos.admin.domain.vo;

import lombok.Data;

import java.util.Date;

@Data
public class AdminTaskVO {
    private Long id;
    private Long userId;
    private String username;
    private String title;
    private Integer status;
    private Date deadline;
    private Long sourceNoteId;
    private Date createTime;
}
