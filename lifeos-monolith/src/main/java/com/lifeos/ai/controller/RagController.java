package com.lifeos.ai.controller;

import com.lifeos.ai.rag.RagQueryRequest;
import com.lifeos.ai.rag.RagQueryResponse;
import com.lifeos.ai.rag.RagQueryService;
import com.lifeos.ai.rag.VectorRepository;
import com.lifeos.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AI RAG Controller
 * Handles RAG-based intelligent Q&A
 */
@RestController
@RequestMapping("/ai/rag")
@RequiredArgsConstructor
@Tag(name = "AI RAG", description = "Retrieval-Augmented Generation for intelligent note search")
public class RagController {

    private final RagQueryService ragQueryService;
    private final VectorRepository vectorRepository;

    @PostMapping("/query")
    @Operation(summary = "RAG Query", description = "Ask questions about your notes using RAG")
    public Result<RagQueryResponse> query(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody RagQueryRequest request) {
        try {
            RagQueryResponse response = ragQueryService.query(userId, request);
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get RAG statistics", description = "Get embedding coverage and stats")
    public Result<Map<String, Object>> getStats(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("embeddingCoverage", vectorRepository.getEmbeddingCoverage(userId));
            return Result.success(stats);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
