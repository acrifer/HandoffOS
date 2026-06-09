package com.lifeos.skill.controller;

import com.lifeos.ai.job.dto.AiJobResponse;
import com.lifeos.common.Result;
import com.lifeos.config.JwtTokenUtil;
import com.lifeos.skill.dto.*;
import com.lifeos.skill.service.SkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/skill")
@RequiredArgsConstructor
@Tag(name = "Handoff Skill", description = "Feishu-to-Dify team handoff skill control plane")
public class SkillController {

    private final SkillService skillService;
    private final JwtTokenUtil jwtTokenUtil;

    @PostMapping
    @Operation(summary = "Create handoff skill")
    public Result<SkillResponse> create(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @RequestBody CreateSkillRequest body) {
        try {
            return Result.success(skillService.create(resolveUserId(servletRequest, userId), body));
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @GetMapping
    @Operation(summary = "List handoff skills")
    public Result<List<SkillResponse>> list(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId) {
        try {
            return Result.success(skillService.list(resolveUserId(servletRequest, userId)));
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @GetMapping("/{skillId}")
    @Operation(summary = "Get handoff skill detail")
    public Result<SkillResponse> detail(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long skillId) {
        try {
            return Result.success(skillService.getDetail(resolveUserId(servletRequest, userId), skillId));
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @PostMapping("/{skillId}/sources/sync")
    @Operation(summary = "Sync Feishu sources into Dify Knowledge")
    public Result<SkillResponse> syncSources(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long skillId,
            @RequestBody SyncSourcesRequest body) {
        try {
            return Result.success(skillService.syncSources(resolveUserId(servletRequest, userId), skillId, body));
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @PostMapping("/{skillId}/distill")
    @Operation(summary = "Distill handoff skill through Dify Workflow")
    public Result<AiJobResponse> distill(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long skillId) {
        try {
            return Result.success(skillService.distill(resolveUserId(servletRequest, userId), skillId));
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @PostMapping("/{skillId}/ask")
    @Operation(summary = "Ask distilled handoff skill")
    public Result<AiJobResponse> ask(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long skillId,
            @RequestBody AskSkillRequest body) {
        try {
            return Result.success(skillService.ask(resolveUserId(servletRequest, userId), skillId, body));
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @GetMapping("/{skillId}/jobs")
    @Operation(summary = "List skill AI jobs")
    public Result<List<AiJobResponse>> jobs(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long skillId,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            return Result.success(skillService.listJobs(resolveUserId(servletRequest, userId), skillId, limit));
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    private Long resolveUserId(HttpServletRequest request, Long userId) {
        if (userId != null) {
            return userId;
        }
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            if (jwtTokenUtil.validateToken(token)) {
                return jwtTokenUtil.getUserIdFromToken(token);
            }
        }
        throw new RuntimeException("Missing userId");
    }
}
