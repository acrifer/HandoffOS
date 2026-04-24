package com.lifeos.note.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

@Data
@TableName("handoff_skill")
public class HandoffSkill {
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String name;

    private String roleDescription;

    private String status;

    private String distillResult;

    private Integer sourceCount;

    private Integer documentSourceCount;

    private Integer chatSourceCount;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long latestJobId;

    private Date createTime;

    private Date updateTime;
}
