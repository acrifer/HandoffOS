package com.lifeos.admin.domain.vo;

import lombok.Data;

import java.util.Date;

@Data
public class AdminRecentIssueVO {
    private String type;
    private String title;
    private String message;
    private Date occurredAt;
}
