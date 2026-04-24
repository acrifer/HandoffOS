package com.lifeos.note.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.api.ai.dto.AiAsyncJobDTO;
import com.lifeos.api.ai.dto.HandoffSkillResultDTO;
import com.lifeos.api.ai.dto.HandoffSkillSourceDTO;
import com.lifeos.note.domain.dto.HandoffSkillAskDTO;
import com.lifeos.note.domain.dto.HandoffSkillCreateDTO;
import com.lifeos.note.domain.dto.HandoffSkillSourceSyncDTO;
import com.lifeos.note.domain.entity.HandoffSkill;
import com.lifeos.note.domain.entity.HandoffSkillChat;
import com.lifeos.note.domain.entity.HandoffSkillSource;
import com.lifeos.note.domain.vo.HandoffSkillChatVO;
import com.lifeos.note.domain.vo.HandoffSkillSourceVO;
import com.lifeos.note.domain.vo.HandoffSkillVO;
import com.lifeos.note.integration.feishu.FeishuClient;
import com.lifeos.note.integration.feishu.FeishuSourcePayload;
import com.lifeos.note.mapper.HandoffSkillChatMapper;
import com.lifeos.note.mapper.HandoffSkillMapper;
import com.lifeos.note.mapper.HandoffSkillSourceMapper;
import com.lifeos.note.service.AiJobService;
import com.lifeos.note.service.HandoffSkillService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class HandoffSkillServiceImpl implements HandoffSkillService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_SOURCES_READY = "SOURCES_READY";
    private static final String STATUS_DISTILLING = "DISTILLING";
    private static final int MAX_AI_SOURCES = 40;
    private static final int MAX_SOURCE_CONTENT = 3000;

    @Resource
    private HandoffSkillMapper handoffSkillMapper;

    @Resource
    private HandoffSkillSourceMapper sourceMapper;

    @Resource
    private HandoffSkillChatMapper chatMapper;

    @Resource
    private AiJobService aiJobService;

    @Resource
    private FeishuClient feishuClient;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public HandoffSkillVO createSkill(Long userId, HandoffSkillCreateDTO request) {
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw new RuntimeException("Skill name is required");
        }

        HandoffSkill skill = new HandoffSkill();
        skill.setId(IdWorker.getId());
        skill.setUserId(userId);
        skill.setName(request.getName().trim());
        skill.setRoleDescription(safe(request.getRoleDescription()));
        skill.setStatus(STATUS_DRAFT);
        skill.setSourceCount(0);
        skill.setDocumentSourceCount(0);
        skill.setChatSourceCount(0);
        handoffSkillMapper.insert(skill);
        return getSkill(userId, skill.getId());
    }

    @Override
    public List<HandoffSkillVO> listSkills(Long userId) {
        LambdaQueryWrapper<HandoffSkill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HandoffSkill::getUserId, userId)
                .orderByDesc(HandoffSkill::getUpdateTime);
        return handoffSkillMapper.selectList(wrapper).stream()
                .map(skill -> toVo(skill, false))
                .toList();
    }

    @Override
    public HandoffSkillVO getSkill(Long userId, Long skillId) {
        return toVo(getOwnedSkill(userId, skillId), true);
    }

    @Override
    public HandoffSkillVO syncSources(Long userId, Long skillId, HandoffSkillSourceSyncDTO request) {
        HandoffSkill skill = getOwnedSkill(userId, skillId);
        if (request == null) {
            throw new RuntimeException("Source sync request is required");
        }

        int inserted = 0;
        List<String> documentRefs = request.getDocumentRefs() == null ? List.of() : request.getDocumentRefs();
        for (String documentRef : documentRefs) {
            if (!StringUtils.hasText(documentRef)) {
                continue;
            }
            inserted += saveSourceIfNew(userId, skill.getId(), feishuClient.fetchDocument(documentRef));
        }

        if (StringUtils.hasText(request.getChatId())) {
            for (FeishuSourcePayload payload : feishuClient.fetchChatMessages(
                    request.getChatId(), request.getStartTime(), request.getEndTime(), request.getLimit())) {
                inserted += saveSourceIfNew(userId, skill.getId(), payload);
            }
        }

        refreshSourceCounts(skill.getId(), inserted > 0 ? STATUS_SOURCES_READY : skill.getStatus(), null);
        return getSkill(userId, skillId);
    }

    @Override
    public AiAsyncJobDTO submitDistill(Long userId, Long skillId) {
        HandoffSkill skill = getOwnedSkill(userId, skillId);
        List<HandoffSkillSourceDTO> sources = listSourceDtos(userId, skillId);
        if (sources.isEmpty()) {
            throw new RuntimeException("At least one source is required before distillation");
        }
        AiAsyncJobDTO job = aiJobService.submitSkillDistillJob(
                userId, skillId, skill.getName(), skill.getRoleDescription(), sources);
        refreshSourceCounts(skillId, STATUS_DISTILLING, job.getId());
        return job;
    }

    @Override
    public AiAsyncJobDTO submitAsk(Long userId, Long skillId, HandoffSkillAskDTO request) {
        HandoffSkill skill = getOwnedSkill(userId, skillId);
        if (request == null || !StringUtils.hasText(request.getQuestion())) {
            throw new RuntimeException("Question is required");
        }
        HandoffSkillResultDTO result = parseResult(skill.getDistillResult());
        if (result == null) {
            throw new RuntimeException("Distill the skill before asking questions");
        }
        return aiJobService.submitSkillAskJob(
                userId, skillId, skill.getName(), skill.getRoleDescription(), request.getQuestion().trim(), result,
                listSourceDtos(userId, skillId));
    }

    private int saveSourceIfNew(Long userId, Long skillId, FeishuSourcePayload payload) {
        if (payload == null || !StringUtils.hasText(payload.getContent())) {
            return 0;
        }
        String hash = feishuClient.contentHash(payload.getSourceType(), payload.getExternalId(), payload.getContent());
        LambdaQueryWrapper<HandoffSkillSource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HandoffSkillSource::getSkillId, skillId)
                .eq(HandoffSkillSource::getContentHash, hash);
        if (sourceMapper.selectCount(wrapper) > 0) {
            return 0;
        }

        HandoffSkillSource source = new HandoffSkillSource();
        source.setId(IdWorker.getId());
        source.setSkillId(skillId);
        source.setUserId(userId);
        source.setSourceType(payload.getSourceType());
        source.setExternalId(payload.getExternalId());
        source.setTitle(payload.getTitle());
        source.setContent(payload.getContent());
        source.setContentHash(hash);
        source.setSourceTime(payload.getSourceTime());
        sourceMapper.insert(source);
        return 1;
    }

    private List<HandoffSkillSourceDTO> listSourceDtos(Long userId, Long skillId) {
        LambdaQueryWrapper<HandoffSkillSource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HandoffSkillSource::getUserId, userId)
                .eq(HandoffSkillSource::getSkillId, skillId)
                .orderByDesc(HandoffSkillSource::getSourceTime)
                .last("LIMIT " + MAX_AI_SOURCES);
        return sourceMapper.selectList(wrapper).stream()
                .map(source -> {
                    HandoffSkillSourceDTO dto = new HandoffSkillSourceDTO();
                    dto.setSourceType(source.getSourceType());
                    dto.setExternalId(source.getExternalId());
                    dto.setTitle(source.getTitle());
                    dto.setContent(truncate(source.getContent(), MAX_SOURCE_CONTENT));
                    dto.setSourceTime(source.getSourceTime());
                    return dto;
                })
                .toList();
    }

    private void refreshSourceCounts(Long skillId, String status, Long latestJobId) {
        long docCount = countSources(skillId, "FEISHU_DOC");
        long chatCount = countSources(skillId, "FEISHU_CHAT");
        LambdaUpdateWrapper<HandoffSkill> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(HandoffSkill::getId, skillId)
                .set(HandoffSkill::getSourceCount, Math.toIntExact(docCount + chatCount))
                .set(HandoffSkill::getDocumentSourceCount, Math.toIntExact(docCount))
                .set(HandoffSkill::getChatSourceCount, Math.toIntExact(chatCount));
        if (StringUtils.hasText(status)) {
            wrapper.set(HandoffSkill::getStatus, status);
        }
        if (latestJobId != null) {
            wrapper.set(HandoffSkill::getLatestJobId, latestJobId);
        }
        handoffSkillMapper.update(null, wrapper);
    }

    private long countSources(Long skillId, String sourceType) {
        LambdaQueryWrapper<HandoffSkillSource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HandoffSkillSource::getSkillId, skillId)
                .eq(HandoffSkillSource::getSourceType, sourceType);
        return sourceMapper.selectCount(wrapper);
    }

    private HandoffSkillVO toVo(HandoffSkill skill, boolean includeDetail) {
        HandoffSkillVO vo = new HandoffSkillVO();
        vo.setId(skill.getId());
        vo.setName(skill.getName());
        vo.setRoleDescription(skill.getRoleDescription());
        vo.setStatus(skill.getStatus());
        vo.setDistillResult(parseResult(skill.getDistillResult()));
        vo.setSourceCount(defaultNumber(skill.getSourceCount()));
        vo.setDocumentSourceCount(defaultNumber(skill.getDocumentSourceCount()));
        vo.setChatSourceCount(defaultNumber(skill.getChatSourceCount()));
        vo.setLatestJobId(skill.getLatestJobId());
        vo.setCreateTime(skill.getCreateTime());
        vo.setUpdateTime(skill.getUpdateTime());
        if (includeDetail) {
            vo.setSources(listSourceVos(skill.getUserId(), skill.getId()));
            vo.setChats(listChatVos(skill.getUserId(), skill.getId()));
        }
        return vo;
    }

    private List<HandoffSkillSourceVO> listSourceVos(Long userId, Long skillId) {
        LambdaQueryWrapper<HandoffSkillSource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HandoffSkillSource::getUserId, userId)
                .eq(HandoffSkillSource::getSkillId, skillId)
                .orderByDesc(HandoffSkillSource::getCreateTime)
                .last("LIMIT 20");
        return sourceMapper.selectList(wrapper).stream()
                .map(source -> {
                    HandoffSkillSourceVO vo = new HandoffSkillSourceVO();
                    vo.setId(source.getId());
                    vo.setSourceType(source.getSourceType());
                    vo.setExternalId(source.getExternalId());
                    vo.setTitle(source.getTitle());
                    vo.setContentPreview(truncate(source.getContent(), 180));
                    vo.setSourceTime(source.getSourceTime());
                    vo.setCreateTime(source.getCreateTime());
                    return vo;
                })
                .toList();
    }

    private List<HandoffSkillChatVO> listChatVos(Long userId, Long skillId) {
        LambdaQueryWrapper<HandoffSkillChat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HandoffSkillChat::getUserId, userId)
                .eq(HandoffSkillChat::getSkillId, skillId)
                .orderByDesc(HandoffSkillChat::getCreateTime)
                .last("LIMIT 20");
        List<HandoffSkillChatVO> values = new ArrayList<>();
        for (HandoffSkillChat chat : chatMapper.selectList(wrapper)) {
            HandoffSkillChatVO vo = new HandoffSkillChatVO();
            vo.setId(chat.getId());
            vo.setJobId(chat.getJobId());
            vo.setQuestion(chat.getQuestion());
            vo.setAnswer(chat.getAnswer());
            if (StringUtils.hasText(chat.getCitations())) {
                vo.setCitations(JSON.parseArray(chat.getCitations(), String.class));
            }
            vo.setCreateTime(chat.getCreateTime());
            values.add(vo);
        }
        return values;
    }

    private HandoffSkill getOwnedSkill(Long userId, Long skillId) {
        LambdaQueryWrapper<HandoffSkill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HandoffSkill::getId, skillId)
                .eq(HandoffSkill::getUserId, userId);
        HandoffSkill skill = handoffSkillMapper.selectOne(wrapper);
        if (skill == null) {
            throw new RuntimeException("Skill not found or access denied");
        }
        return skill;
    }

    private HandoffSkillResultDTO parseResult(String payload) {
        if (!StringUtils.hasText(payload)) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, HandoffSkillResultDTO.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private int defaultNumber(Integer value) {
        return value == null ? 0 : value;
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength).trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
