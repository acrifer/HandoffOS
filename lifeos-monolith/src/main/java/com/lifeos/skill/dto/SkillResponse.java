package com.lifeos.skill.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class SkillResponse {
    private Long id;
    private String name;
    private String roleDescription;
    private String status;
    private Map<String, Object> distillResult;
    private Integer sourceCount;
    private Integer documentSourceCount;
    private Integer chatSourceCount;
    private Long latestJobId;
    private String difyDatasetId;
    private LocalDateTime lastSyncTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<SkillSourceResponse> sources = new ArrayList<>();
    private List<SkillChatResponse> chats = new ArrayList<>();
}
