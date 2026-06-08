package com.lifeos.ai.knowledgebase.controller;

import com.lifeos.ai.knowledgebase.dto.AdminAiStatsResponse;
import com.lifeos.ai.knowledgebase.dto.LogAnalysisRequest;
import com.lifeos.ai.knowledgebase.dto.LogAnalysisResponse;
import com.lifeos.ai.knowledgebase.service.AiKnowledgeService;
import com.lifeos.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin/ai")
@RequiredArgsConstructor
public class AdminAiController {

    private final AiKnowledgeService aiKnowledgeService;

    @GetMapping("/stats")
    public Result<AdminAiStatsResponse> stats(
            @RequestParam(required = false) LocalDateTime start,
            @RequestParam(required = false) LocalDateTime end,
            @RequestParam(required = false) Long skillId) {
        try {
            return Result.success(aiKnowledgeService.adminStats(start, end, skillId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/log-analysis")
    public Result<LogAnalysisResponse> logAnalysis(@RequestBody(required = false) LogAnalysisRequest body) {
        try {
            return Result.success(aiKnowledgeService.analyzeLogs(body == null ? new LogAnalysisRequest() : body));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
