package com.lifeos.note.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Date;

@Data
public class HandoffSkillSourceVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String sourceType;
    private String externalId;
    private String title;
    private String contentPreview;
    private Date sourceTime;
    private Date createTime;
}
