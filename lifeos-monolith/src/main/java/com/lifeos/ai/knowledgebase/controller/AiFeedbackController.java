package com.lifeos.ai.knowledgebase.controller;

import com.lifeos.ai.knowledgebase.dto.FeedbackRequest;
import com.lifeos.ai.knowledgebase.service.AiKnowledgeService;
import com.lifeos.common.Result;
import com.lifeos.config.JwtTokenUtil;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/ai/qa")
@RequiredArgsConstructor
public class AiFeedbackController {

    private final AiKnowledgeService aiKnowledgeService;
    private final JwtTokenUtil jwtTokenUtil;

    @PostMapping("/{qaLogId}/feedback")
    public Result<Map<String, Object>> feedback(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long qaLogId,
            @RequestBody FeedbackRequest body) {
        try {
            return Result.success(aiKnowledgeService.feedback(resolveUserId(servletRequest, userId), qaLogId, body));
        } catch (Exception e) {
            return Result.error(e.getMessage());
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
