package com.lifeos.note.controller;

import com.lifeos.common.Result;
import com.lifeos.note.dto.NoteRequest;
import com.lifeos.note.dto.NoteResponse;
import com.lifeos.note.service.NoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

    @PostMapping
    @Operation(summary = "Create note")
    public Result<NoteResponse> createNote(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody NoteRequest request) {
        try {
            return Result.success(noteService.createNote(userId, request));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{noteId}")
    @Operation(summary = "Update note")
    public Result<NoteResponse> updateNote(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long noteId,
            @RequestBody NoteRequest request) {
        try {
            return Result.success(noteService.updateNote(userId, noteId, request));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{noteId}")
    @Operation(summary = "Delete note")
    public Result<Void> deleteNote(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long noteId) {
        try {
            noteService.deleteNote(userId, noteId);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{noteId}")
    @Operation(summary = "Get note by ID")
    public Result<NoteResponse> getNote(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long noteId) {
        try {
            return Result.success(noteService.getNote(userId, noteId));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping
    @Operation(summary = "List notes")
    public Result<Page<NoteResponse>> listNotes(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return Result.success(noteService.listNotes(userId, page, size));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Search notes by keyword")
    public Result<Page<NoteResponse>> searchNotes(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            return Result.success(noteService.searchNotes(userId, keyword, page, size));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
