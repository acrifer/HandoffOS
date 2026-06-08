package com.lifeos.ai.knowledgebase.controller;

import com.lifeos.ai.knowledgebase.dto.*;
import com.lifeos.ai.knowledgebase.service.AiKnowledgeService;
import com.lifeos.common.Result;
import com.lifeos.config.JwtTokenUtil;
import com.lifeos.skill.dto.AskSkillRequest;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/skills")
@RequiredArgsConstructor
public class SkillKnowledgeController {

    private final AiKnowledgeService aiKnowledgeService;
    private final JwtTokenUtil jwtTokenUtil;

    @PostMapping("/{skillId}/documents")
    public Result<KnowledgeDocumentResponse> createDocument(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long skillId,
            @RequestBody KnowledgeDocumentRequest body) {
        try {
            return Result.success(aiKnowledgeService.createDocument(resolveUserId(servletRequest, userId), skillId, body));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{skillId}/documents")
    public Result<List<KnowledgeDocumentResponse>> listDocuments(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long skillId) {
        try {
            return Result.success(aiKnowledgeService.listDocuments(resolveUserId(servletRequest, userId), skillId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{skillId}/documents/{documentId}/parse")
    public Result<KnowledgeDocumentResponse> parseDocument(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long skillId,
            @PathVariable Long documentId,
            @RequestBody(required = false) ParseDocumentRequest body) {
        try {
            return Result.success(aiKnowledgeService.parseDocument(
                    resolveUserId(servletRequest, userId),
                    skillId,
                    documentId,
                    body == null ? new ParseDocumentRequest() : body
            ));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{skillId}/documents/{documentId}/vectorize")
    public Result<Map<String, Object>> vectorizeDocument(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long skillId,
            @PathVariable Long documentId,
            @RequestBody(required = false) VectorizeDocumentRequest body) {
        try {
            return Result.success(aiKnowledgeService.vectorizeDocument(
                    resolveUserId(servletRequest, userId),
                    skillId,
                    documentId,
                    body == null ? new VectorizeDocumentRequest() : body
            ));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{skillId}/summary")
    public Result<KnowledgeDocumentResponse> summary(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long skillId) {
        try {
            return Result.success(aiKnowledgeService.summarizeSkill(resolveUserId(servletRequest, userId), skillId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{skillId}/ask")
    public Result<QaAnswerResponse> ask(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long skillId,
            @RequestBody AskSkillRequest body) {
        try {
            return Result.success(aiKnowledgeService.ask(resolveUserId(servletRequest, userId), skillId, body));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{skillId}/chat")
    public Result<QaAnswerResponse> chat(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long skillId,
            @RequestBody AskSkillRequest body) {
        return ask(servletRequest, userId, skillId, body);
    }

    @GetMapping("/{skillId}/qa-history")
    public Result<List<QaLogResponse>> history(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long skillId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        try {
            return Result.success(aiKnowledgeService.history(resolveUserId(servletRequest, userId), skillId, page, size, status));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{skillId}/search")
    public Result<List<SearchResultResponse>> search(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long skillId,
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "8") int limit) {
        try {
            return Result.success(aiKnowledgeService.search(resolveUserId(servletRequest, userId), skillId, query, limit));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{skillId}/recommended-questions")
    public Result<List<RecommendedQuestionResponse>> recommendedQuestions(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long skillId) {
        try {
            return Result.success(aiKnowledgeService.recommendedQuestions(resolveUserId(servletRequest, userId), skillId));
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
