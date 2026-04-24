package com.lifeos.note.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

@Data
@TableName("handoff_skill_source")
public class HandoffSkillSource {
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long skillId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String sourceType;

    private String externalId;

    private String title;

    private String content;

    private String contentHash;

    private Date sourceTime;

    private Date createTime;
}
