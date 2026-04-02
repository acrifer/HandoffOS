package com.lifeos.admin.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("admin_role")
public class AdminRole {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String roleCode;
    private String roleName;
    private Date createTime;
}
