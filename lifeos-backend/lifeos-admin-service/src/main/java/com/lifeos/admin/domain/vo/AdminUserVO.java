package com.lifeos.admin.domain.vo;

import lombok.Data;

import java.util.Date;

@Data
public class AdminUserVO {
    private Long id;
    private String username;
    private String email;
    private Boolean enabled;
    private Date createTime;
}
