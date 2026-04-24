package com.lifeos.api.ai.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class AiAsyncJobResultDTO implements Serializable {
    private String summary;
    private String suggestedTitle;
    private String suggestedTags;
    private List<AiTaskSuggestionDTO> tasks = new ArrayList<>();
    private AiWeeklyReviewResultDTO weeklyReview;
    private HandoffSkillResultDTO handoffSkill;
    private String skillAnswer;
    private List<String> citations = new ArrayList<>();
}
