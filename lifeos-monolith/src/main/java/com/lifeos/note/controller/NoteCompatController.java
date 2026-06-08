package com.lifeos.note.controller;

import com.lifeos.common.Result;
import com.lifeos.config.JwtTokenUtil;
import com.lifeos.note.dto.NoteRequest;
import com.lifeos.note.dto.NoteResponse;
import com.lifeos.note.entity.Note;
import com.lifeos.note.repository.NoteRepository;
import com.lifeos.note.service.NoteService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Compatibility endpoints for the current Vue app, whose legacy API prefix is /note.
 */
@RestController
@RequestMapping("/note")
@RequiredArgsConstructor
public class NoteCompatController {

    private final NoteRepository noteRepository;
    private final NoteService noteService;
    private final JwtTokenUtil jwtTokenUtil;

    @PostMapping
    public Result<Long> create(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @RequestBody NoteRequest request) {
        try {
            return Result.success(noteService.createNote(resolveUserId(servletRequest, userId), request).getId());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping
    public Result<NoteResponse> update(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @RequestBody NoteRequest request) {
        try {
            if (request.getId() == null) {
                throw new RuntimeException("Note id is required");
            }
            return Result.success(noteService.updateNote(resolveUserId(servletRequest, userId), request.getId(), request));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{noteId}")
    public Result<Void> delete(
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

    @GetMapping("/list")
    public Result<List<NoteResponse>> list(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId) {
        try {
            return Result.success(sortedNotes(resolveUserId(servletRequest, userId)).stream().map(this::toResponse).toList());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/search")
    public Result<List<NoteResponse>> search(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) Boolean pinned,
            @RequestParam(required = false) Boolean needsOrganization,
            @RequestParam(required = false) Boolean hasSummary) {
        try {
            Long resolvedUserId = resolveUserId(servletRequest, userId);
            String keywordValue = keyword == null ? "" : keyword.trim().toLowerCase();
            String tagValue = tags == null ? "" : tags.trim().toLowerCase();
            List<NoteResponse> notes = sortedNotes(resolvedUserId).stream()
                    .filter(note -> keywordValue.isBlank()
                            || contains(note.getTitle(), keywordValue)
                            || contains(note.getContent(), keywordValue)
                            || contains(note.getTags(), keywordValue)
                            || contains(note.getSummary(), keywordValue))
                    .filter(note -> tagValue.isBlank() || contains(note.getTags(), tagValue))
                    .filter(note -> pinned == null || pinned.equals(note.getPinned()))
                    .filter(note -> hasSummary == null || hasSummary.equals(note.getSummary() != null && !note.getSummary().isBlank()))
                    .filter(note -> needsOrganization == null || !needsOrganization || note.getSummary() == null || note.getSummary().isBlank())
                    .map(this::toResponse)
                    .toList();
            return Result.success(notes);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{noteId}")
    public Result<NoteResponse> detail(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long noteId) {
        try {
            Long resolvedUserId = resolveUserId(servletRequest, userId);
            Note note = noteRepository.findByIdAndUserId(noteId, resolvedUserId)
                    .orElseThrow(() -> new RuntimeException("Note not found"));
            return Result.success(toResponse(note));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{noteId}/pin")
    public Result<NoteResponse> pin(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long noteId,
            @RequestBody Map<String, Boolean> body) {
        try {
            Long resolvedUserId = resolveUserId(servletRequest, userId);
            Note note = noteRepository.findByIdAndUserId(noteId, resolvedUserId)
                    .orElseThrow(() -> new RuntimeException("Note not found"));
            note.setPinned(Boolean.TRUE.equals(body.get("pinned")));
            noteRepository.save(note);
            return Result.success(toResponse(note));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{noteId}/review")
    public Result<NoteResponse> review(
            HttpServletRequest servletRequest,
            @Parameter(hidden = true) @RequestAttribute(value = "userId", required = false) Long userId,
            @PathVariable Long noteId,
            @RequestBody(required = false) NoteRequest request) {
        try {
            Long resolvedUserId = resolveUserId(servletRequest, userId);
            Note note = noteRepository.findByIdAndUserId(noteId, resolvedUserId)
                    .orElseThrow(() -> new RuntimeException("Note not found"));
            NoteRequest update = request == null ? new NoteRequest() : request;
            update.setId(noteId);
            update.setTitle(note.getTitle());
            update.setContent(note.getContent());
            update.setTags(note.getTags());
            update.setSummary(note.getSummary());
            update.setPinned(note.getPinned());
            if (update.getLastReviewedAt() == null && update.getReviewState() != null) {
                update.setLastReviewedAt(java.time.LocalDateTime.now().toString());
            }
            return Result.success(noteService.updateNote(resolvedUserId, noteId, update));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private List<Note> sortedNotes(Long userId) {
        return noteRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(Note::getPinned, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Note::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private NoteResponse toResponse(Note note) {
        NoteResponse response = new NoteResponse();
        BeanUtils.copyProperties(note, response);
        return response;
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
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
