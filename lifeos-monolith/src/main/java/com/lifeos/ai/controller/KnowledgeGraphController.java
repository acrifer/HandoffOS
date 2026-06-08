package com.lifeos.ai.controller;

import com.lifeos.ai.knowledge.ConflictDetectionService;
import com.lifeos.ai.knowledge.ConflictResult;
import com.lifeos.ai.knowledge.KnowledgeGraphService;
import com.lifeos.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Knowledge Graph Controller
 * Handles knowledge graph operations for handoff skills
 */
@RestController
@RequestMapping("/ai/knowledge")
@RequiredArgsConstructor
@Tag(name = "Knowledge Graph", description = "Knowledge graph and conflict detection")
public class KnowledgeGraphController {

    private final KnowledgeGraphService knowledgeGraphService;
    private final ConflictDetectionService conflictDetectionService;

    @GetMapping("/graph/{skillId}")
    @Operation(summary = "Get knowledge graph", description = "Get entities and relations for a skill")
    public Result<Map<String, Object>> getKnowledgeGraph(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long skillId) {
        try {
            Map<String, Object> graph = knowledgeGraphService.getKnowledgeGraph(skillId);
            return Result.success(graph);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/build/{skillId}")
    @Operation(summary = "Build knowledge graph", description = "Extract entities and relations from skill sources")
    public Result<Map<String, Object>> buildKnowledgeGraph(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long skillId,
            @RequestBody List<String> sourceTexts) {
        try {
            knowledgeGraphService.buildKnowledgeGraph(skillId, sourceTexts);
            Map<String, Object> graph = knowledgeGraphService.getKnowledgeGraph(skillId);
            return Result.success(graph);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/conflicts/detect")
    @Operation(summary = "Detect conflicts", description = "Detect conflicts between multiple sources")
    public Result<List<ConflictResult>> detectConflicts(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody List<ConflictDetectionService.SourceContent> sources) {
        try {
            List<ConflictResult> conflicts = conflictDetectionService.detectConflicts(sources);
            return Result.success(conflicts);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
