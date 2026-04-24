package com.lifeos.note.service;

import com.lifeos.api.ai.dto.AiAsyncJobDTO;
import com.lifeos.note.domain.dto.HandoffSkillAskDTO;
import com.lifeos.note.domain.dto.HandoffSkillCreateDTO;
import com.lifeos.note.domain.dto.HandoffSkillSourceSyncDTO;
import com.lifeos.note.domain.vo.HandoffSkillVO;

import java.util.List;

public interface HandoffSkillService {
    HandoffSkillVO createSkill(Long userId, HandoffSkillCreateDTO request);

    List<HandoffSkillVO> listSkills(Long userId);

    HandoffSkillVO getSkill(Long userId, Long skillId);

    HandoffSkillVO syncSources(Long userId, Long skillId, HandoffSkillSourceSyncDTO request);

    AiAsyncJobDTO submitDistill(Long userId, Long skillId);

    AiAsyncJobDTO submitAsk(Long userId, Long skillId, HandoffSkillAskDTO request);
}
