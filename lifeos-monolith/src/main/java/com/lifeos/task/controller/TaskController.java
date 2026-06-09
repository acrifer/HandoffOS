package com.lifeos.task.controller;

import com.lifeos.common.Result;
import com.lifeos.config.JwtTokenUtil;
import com.lifeos.task.dto.TaskRequest;
import com.lifeos.task.dto.TaskResponse;
import com.lifeos.task.service.TaskService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final JwtTokenUtil jwtTokenUtil;

    @PostMapping
    public Result<TaskResponse> create(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @RequestBody TaskRequest request) {
        try {
            return Result.success(taskService.createTask(resolveUserId(servletRequest, userId), request));
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @PutMapping
    public Result<TaskResponse> update(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @RequestBody TaskRequest request) {
        try {
            return Result.success(taskService.updateTask(resolveUserId(servletRequest, userId), request));
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @DeleteMapping("/{taskId}")
    public Result<Void> delete(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long taskId) {
        try {
            taskService.deleteTask(resolveUserId(servletRequest, userId), taskId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @GetMapping("/list")
    public Result<List<TaskResponse>> list(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @RequestParam(required = false) Long skillId) {
        try {
            return Result.success(taskService.listTasks(resolveUserId(servletRequest, userId), skillId));
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @PostMapping("/{taskId}/complete")
    public Result<TaskResponse> complete(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long taskId) {
        try {
            return Result.success(taskService.completeTask(resolveUserId(servletRequest, userId), taskId));
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
