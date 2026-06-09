package com.lifeos.ai.job.controller;

import com.lifeos.ai.job.dto.AiJobResponse;
import com.lifeos.ai.job.service.AiWorkflowJobService;
import com.lifeos.common.Result;
import com.lifeos.config.JwtTokenUtil;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/note/jobs")
@RequiredArgsConstructor
public class AiWorkflowJobController {

    private final AiWorkflowJobService jobService;
    private final JwtTokenUtil jwtTokenUtil;

    @GetMapping
    public Result<List<AiJobResponse>> listJobs(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @RequestParam(required = false) String jobType,
            @RequestParam(defaultValue = "30") int limit) {
        try {
            return Result.success(jobService.listJobs(resolveUserId(servletRequest, userId), null, jobType, limit));
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @GetMapping("/{jobId}")
    public Result<AiJobResponse> getJob(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long jobId) {
        try {
            return Result.success(jobService.getJob(resolveUserId(servletRequest, userId), jobId));
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
            return jwtTokenUtil.getUserIdFromToken(bearerToken.substring(7));
        }
        throw new RuntimeException("Missing userId");
    }
}
