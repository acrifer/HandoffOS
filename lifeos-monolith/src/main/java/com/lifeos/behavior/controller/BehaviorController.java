package com.lifeos.behavior.controller;

import com.lifeos.common.Result;
import com.lifeos.note.entity.Note;
import com.lifeos.note.repository.NoteRepository;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/behavior")
@RequiredArgsConstructor
public class BehaviorController {

    private final NoteRepository noteRepository;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        try {
            List<Note> notes = noteRepository.findByUserId(userId);
            LocalDateTime weekStart = LocalDateTime.now().minusDays(7);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("noteCount", notes.size());
            data.put("weekNewNoteCount", notes.stream().filter(note -> note.getCreateTime() != null && note.getCreateTime().isAfter(weekStart)).count());
            data.put("weekOrganizedNoteCount", notes.stream().filter(note -> note.getSummary() != null && !note.getSummary().isBlank()).count());
            data.put("aiInboxCount", notes.stream().filter(note -> note.getSummary() == null || note.getSummary().isBlank()).count());
            data.put("notesToReviewCount", 0);
            data.put("pendingTaskCount", 0);
            data.put("pendingExtractedTaskCount", 0);
            data.put("weekCompletedTaskCount", 0);
            data.put("recentNotes", notes.stream()
                    .sorted(Comparator.comparing(Note::getUpdateTime, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(6)
                    .map(this::noteSummary)
                    .toList());
            data.put("topTags", topTags(notes));
            data.put("recentTrend", recentTrend(notes));
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private Map<String, Object> noteSummary(Note note) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", note.getId());
        map.put("title", note.getTitle());
        map.put("tags", note.getTags());
        map.put("updatedAt", note.getUpdateTime());
        return map;
    }

    private List<Map<String, Object>> topTags(List<Note> notes) {
        Map<String, Long> counts = notes.stream()
                .flatMap(note -> Arrays.stream(Optional.ofNullable(note.getTags()).orElse("").split(",")))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .map(entry -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("tag", entry.getKey());
                    map.put("count", entry.getValue());
                    return map;
                })
                .toList();
    }

    private List<Map<String, Object>> recentTrend(List<Note> notes) {
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            long count = notes.stream()
                    .filter(note -> note.getCreateTime() != null && note.getCreateTime().toLocalDate().equals(day))
                    .count();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", day.toString().substring(5));
            item.put("count", count);
            trend.add(item);
        }
        return trend;
    }
}
