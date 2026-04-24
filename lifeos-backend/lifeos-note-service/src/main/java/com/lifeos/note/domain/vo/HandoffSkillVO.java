package com.lifeos.note.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.lifeos.api.ai.dto.HandoffSkillResultDTO;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class HandoffSkillVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String name;
    private String roleDescription;
    private String status;
    private HandoffSkillResultDTO distillResult;
    private Integer sourceCount;
    private Integer documentSourceCount;
    private Integer chatSourceCount;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long latestJobId;

    private List<HandoffSkillSourceVO> sources = new ArrayList<>();
    private List<HandoffSkillChatVO> chats = new ArrayList<>();
    private Date createTime;
    private Date updateTime;
}
