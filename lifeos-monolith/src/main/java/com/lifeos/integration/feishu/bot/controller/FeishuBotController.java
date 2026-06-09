package com.lifeos.integration.feishu.bot.controller;

import com.lifeos.common.Result;
import com.lifeos.config.JwtTokenUtil;
import com.lifeos.integration.feishu.bot.dto.FeishuBotEventResponse;
import com.lifeos.integration.feishu.bot.dto.FeishuBotStatusResponse;
import com.lifeos.integration.feishu.bot.dto.FeishuChatBindingRequest;
import com.lifeos.integration.feishu.bot.dto.FeishuChatBindingResponse;
import com.lifeos.integration.feishu.bot.service.FeishuBotBindingService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/feishu/bot")
@RequiredArgsConstructor
public class FeishuBotController {

    private final FeishuBotBindingService bindingService;
    private final JwtTokenUtil jwtTokenUtil;

    @GetMapping("/status")
    public Result<FeishuBotStatusResponse> status() {
        try {
            return Result.success(bindingService.status());
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @PostMapping("/bindings")
    public Result<FeishuChatBindingResponse> bind(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @RequestBody FeishuChatBindingRequest request) {
        try {
            return Result.success(bindingService.bind(resolveUserId(servletRequest, userId), request));
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @GetMapping("/bindings")
    public Result<List<FeishuChatBindingResponse>> bindings(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @RequestParam(required = false) Long skillId) {
        try {
            return Result.success(bindingService.listBindings(resolveUserId(servletRequest, userId), skillId));
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @DeleteMapping("/bindings/{bindingId}")
    public Result<Void> disableBinding(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long bindingId) {
        try {
            bindingService.disable(resolveUserId(servletRequest, userId), bindingId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @GetMapping("/events")
    public Result<List<FeishuBotEventResponse>> events(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @RequestParam(required = false) Long skillId,
            @RequestParam(defaultValue = "30") int limit) {
        try {
            return Result.success(bindingService.listEvents(resolveUserId(servletRequest, userId), skillId, limit));
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
