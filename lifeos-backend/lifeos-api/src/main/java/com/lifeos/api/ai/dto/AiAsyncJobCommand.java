package com.lifeos.api.ai.dto;

import com.lifeos.api.behavior.dto.DashboardStatsDTO;
import lombok.Data;

import java.io.Serializable;

@Data
public class AiAsyncJobCommand implements Serializable {
    private Long jobId;
    private Long userId;
    private Long noteId;
    private Long skillId;
    private String jobType;
    private String title;
    private String content;
    private String tags;
    private String skillName;
    private String roleDescription;
    private String question;
    private HandoffSkillResultDTO handoffSkill;
    private java.util.List<HandoffSkillSourceDTO> skillSources = new java.util.ArrayList<>();
    private DashboardStatsDTO dashboard;
}
