package com.lifeos.api.ai.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class HandoffSkillSourceDTO implements Serializable {
    private String sourceType;
    private String externalId;
    private String title;
    private String content;
    private Date sourceTime;
}
