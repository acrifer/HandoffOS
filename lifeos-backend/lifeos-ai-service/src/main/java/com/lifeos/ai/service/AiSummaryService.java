package com.lifeos.ai.service;

import com.lifeos.api.ai.dto.AiAsyncJobCommand;
import com.lifeos.api.ai.dto.AiSummaryCommand;
import com.lifeos.api.ai.dto.AiWeeklyReviewResultDTO;
import com.lifeos.api.ai.dto.HandoffSkillResultDTO;
import com.lifeos.api.ai.dto.AiNoteOrganizeResult;
import com.lifeos.api.ai.dto.AiSummaryResult;
import com.lifeos.api.ai.dto.AiTaskExtractionResult;

public interface AiSummaryService {
    AiSummaryResult generateSummary(AiSummaryCommand command);

    AiNoteOrganizeResult organizeNote(AiSummaryCommand command);

    AiTaskExtractionResult extractTasks(AiSummaryCommand command);

    AiWeeklyReviewResultDTO generateWeeklyReview(AiAsyncJobCommand command);

    HandoffSkillResultDTO distillHandoffSkill(AiAsyncJobCommand command);

    String answerHandoffSkillQuestion(AiAsyncJobCommand command);
}
