package com.lifeos.note.controller;

import com.lifeos.common.Result;
import com.lifeos.config.JwtTokenUtil;
import com.lifeos.note.dto.NoteRequest;
import com.lifeos.note.dto.NoteResponse;
import com.lifeos.note.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Note Controller
 */
@RestController
@RequestMapping("/notes")
@RequiredArgsConstructor
@Tag(name = "Notes", description = "Note management")
public class NoteController {

    private final NoteService noteService;
    private final JwtTokenUtil jwtTokenUtil;

    @PostMapping
    @Operation(summary = "Create note")
    public Result<NoteResponse> createNote(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @RequestBody NoteRequest request) {
        try {
            return Result.success(noteService.createNote(resolveUserId(servletRequest, userId), request));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{noteId}")
    @Operation(summary = "Update note")
    public Result<NoteResponse> updateNote(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long noteId,
            @RequestBody NoteRequest request) {
        try {
            return Result.success(noteService.updateNote(resolveUserId(servletRequest, userId), noteId, request));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{noteId}")
    @Operation(summary = "Delete note")
    public Result<Void> deleteNote(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long noteId) {
        try {
            noteService.deleteNote(resolveUserId(servletRequest, userId), noteId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{noteId}")
    @Operation(summary = "Get note by ID")
    public Result<NoteResponse> getNote(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long noteId) {
        try {
            return Result.success(noteService.getNote(resolveUserId(servletRequest, userId), noteId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "List notes")
    public Result<Page<NoteResponse>> listNotes(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return Result.success(noteService.listNotes(resolveUserId(servletRequest, userId), page, size));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Search notes by keyword")
    public Result<Page<NoteResponse>> searchNotes(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return Result.success(noteService.searchNotes(resolveUserId(servletRequest, userId), keyword, page, size));
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
            return jwtTokenUtil.getUserIdFromToken(bearerToken.substring(7));
        }
        throw new RuntimeException("Missing userId");
    }
}
