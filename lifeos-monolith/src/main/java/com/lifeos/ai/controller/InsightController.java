package com.lifeos.ai.controller;

import com.lifeos.ai.insight.InsightEngine;
import com.lifeos.ai.insight.InsightReport;
import com.lifeos.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Insight Controller
 * Provides learning insights and recommendations
 */
@RestController
@RequestMapping("/ai/insight")
@RequiredArgsConstructor
@Tag(name = "Learning Insights", description = "Learning pattern analysis and recommendations")
public class InsightController {

    private final InsightEngine insightEngine;

    @GetMapping("/weekly")
    @Operation(summary = "Get weekly insight report", description = "Generate weekly learning insights")
    public Result<InsightReport> getWeeklyReport(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        try {
            InsightReport report = insightEngine.generateWeeklyReport(userId);
            return Result.success(report);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/monthly")
    @Operation(summary = "Get monthly insight report", description = "Generate monthly learning insights")
    public Result<InsightReport> getMonthlyReport(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        try {
            InsightReport report = insightEngine.generateMonthlyReport(userId);
            return Result.success(report);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
