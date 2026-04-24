package com.lifeos.note.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lifeos.api.ai.dto.AiAsyncJobDTO;
import com.lifeos.api.ai.dto.AiAsyncJobUpdateDTO;
import com.lifeos.api.ai.dto.HandoffSkillResultDTO;
import com.lifeos.api.ai.dto.HandoffSkillSourceDTO;
import com.lifeos.note.domain.entity.AiWorkflowJob;

import java.util.List;

public interface AiJobService extends IService<AiWorkflowJob> {

    AiAsyncJobDTO submitNoteJob(Long userId, Long noteId, String jobType);

    AiAsyncJobDTO submitWeeklyReview(Long userId);

    AiAsyncJobDTO submitSkillDistillJob(Long userId, Long skillId, String skillName, String roleDescription,
            List<HandoffSkillSourceDTO> sources);

    AiAsyncJobDTO submitSkillAskJob(Long userId, Long skillId, String skillName, String roleDescription,
            String question, HandoffSkillResultDTO handoffSkill, List<HandoffSkillSourceDTO> sources);

    AiAsyncJobDTO getJob(Long userId, Long jobId);

    List<AiAsyncJobDTO> listJobs(Long userId, Long noteId, String jobType, Integer limit);

    List<AiAsyncJobDTO> listSkillJobs(Long userId, Long skillId, Integer limit);

    void updateJobStatus(Long jobId, AiAsyncJobUpdateDTO request);
}
