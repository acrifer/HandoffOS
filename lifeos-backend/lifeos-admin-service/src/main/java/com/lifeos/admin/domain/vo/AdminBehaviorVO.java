package com.lifeos.admin.domain.vo;

import lombok.Data;

import java.util.Date;

@Data
public class AdminBehaviorVO {
    private Long id;
    private String eventId;
    private Long userId;
    private String username;
    private String actionType;
    private Long targetId;
    private Date createTime;
}
