package com.lifeos.api.ai.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class AiAsyncJobDTO implements Serializable {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long noteId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long skillId;

    private String noteTitle;
    private String skillName;
    private String jobType;
    private String status;
    private String errorMessage;
    private AiAsyncJobResultDTO result;
    private Date createTime;
    private Date updateTime;
    private Date finishedTime;
}
