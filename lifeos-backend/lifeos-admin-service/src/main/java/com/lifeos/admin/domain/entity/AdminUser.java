package com.lifeos.admin.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("admin_user")
public class AdminUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String displayName;
    private String email;
    private Boolean enabled;
    private Date lastLoginTime;
    private Date createTime;
    private Date updateTime;
}
