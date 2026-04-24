package com.lifeos.note.controller;

import com.lifeos.api.ai.dto.AiAsyncJobDTO;
import com.lifeos.common.response.Result;
import com.lifeos.note.domain.dto.HandoffSkillAskDTO;
import com.lifeos.note.domain.dto.HandoffSkillCreateDTO;
import com.lifeos.note.domain.dto.HandoffSkillSourceSyncDTO;
import com.lifeos.note.domain.vo.HandoffSkillVO;
import com.lifeos.note.service.AiJobService;
import com.lifeos.note.service.HandoffSkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/skill")
@Slf4j
@Tag(name = "团队交接 Skill", description = "基于飞书资料蒸馏项目/角色交接助手")
public class HandoffSkillController {

    @Resource
    private HandoffSkillService handoffSkillService;

    @Resource
    private AiJobService aiJobService;

    @PostMapping
    @Operation(summary = "创建交接 Skill", description = "创建一个项目/角色交接助手，不绑定具体个人数字人。")
    public Result<HandoffSkillVO> createSkill(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @RequestBody HandoffSkillCreateDTO request) {
        try {
            return Result.success(handoffSkillService.createSkill(userId, request));
        } catch (Exception ex) {
            log.error("Failed to create handoff skill", ex);
            return Result.error(ex.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "查询交接 Skill 列表", description = "返回当前用户创建的交接助手。")
    public Result<List<HandoffSkillVO>> listSkills(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        try {
            return Result.success(handoffSkillService.listSkills(userId));
        } catch (Exception ex) {
            log.error("Failed to list handoff skills", ex);
            return Result.error(ex.getMessage());
        }
    }

    @GetMapping("/{skillId}")
    @Operation(summary = "查询交接 Skill 详情", description = "返回蒸馏结果、来源预览和问答记录。")
    public Result<HandoffSkillVO> getSkill(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @PathVariable("skillId") Long skillId) {
        try {
            return Result.success(handoffSkillService.getSkill(userId, skillId));
        } catch (Exception ex) {
            log.error("Failed to get handoff skill {}", skillId, ex);
            return Result.error(ex.getMessage());
        }
    }

    @PostMapping("/{skillId}/sources/sync")
    @Operation(summary = "同步飞书来源", description = "按用户提供的文档链接/ID 和群聊 chat_id 拉取交接材料。")
    public Result<HandoffSkillVO> syncSources(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @PathVariable("skillId") Long skillId,
            @RequestBody HandoffSkillSourceSyncDTO request) {
        try {
            return Result.success(handoffSkillService.syncSources(userId, skillId, request));
        } catch (Exception ex) {
            log.error("Failed to sync handoff skill sources {}", skillId, ex);
            return Result.error(ex.getMessage());
        }
    }

    @PostMapping("/{skillId}/distill")
    @Operation(summary = "创建 Skill 蒸馏作业", description = "异步生成角色边界、工作规则、决策偏好和交接清单。")
    public Result<AiAsyncJobDTO> distillSkill(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @PathVariable("skillId") Long skillId) {
        try {
            return Result.success(handoffSkillService.submitDistill(userId, skillId));
        } catch (Exception ex) {
            log.error("Failed to distill handoff skill {}", skillId, ex);
            return Result.error(ex.getMessage());
        }
    }

    @PostMapping("/{skillId}/ask")
    @Operation(summary = "创建 Skill 问答作业", description = "基于已蒸馏的交接 Skill 异步回答问题。")
    public Result<AiAsyncJobDTO> askSkill(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @PathVariable("skillId") Long skillId,
            @RequestBody HandoffSkillAskDTO request) {
        try {
            return Result.success(handoffSkillService.submitAsk(userId, skillId, request));
        } catch (Exception ex) {
            log.error("Failed to ask handoff skill {}", skillId, ex);
            return Result.error(ex.getMessage());
        }
    }

    @GetMapping("/{skillId}/jobs")
    @Operation(summary = "查询 Skill 作业", description = "返回指定 Skill 的蒸馏和问答作业历史。")
    public Result<List<AiAsyncJobDTO>> listSkillJobs(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
            @PathVariable("skillId") Long skillId,
            @RequestParam(name = "limit", required = false) Integer limit) {
        try {
            return Result.success(aiJobService.listSkillJobs(userId, skillId, limit));
        } catch (Exception ex) {
            log.error("Failed to list handoff skill jobs {}", skillId, ex);
            return Result.error(ex.getMessage());
        }
    }
}
