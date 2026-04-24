package com.lifeos.ai.insight;

import com.lifeos.note.entity.Note;
import com.lifeos.note.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Learning Pattern Analyzer
 * Analyzes user's learning patterns from notes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningPatternAnalyzer {

    private final NoteRepository noteRepository;

    /**
     * Analyze learning patterns for a user
     */
    public List<LearningPattern> analyzePatterns(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        List<Note> notes = noteRepository.findByUserId(userId).stream()
                .filter(note -> note.getCreateTime().isAfter(startTime) && note.getCreateTime().isBefore(endTime))
                .collect(Collectors.toList());

        if (notes.isEmpty()) {
            return new ArrayList<>();
        }

        List<LearningPattern> patterns = new ArrayList<>();

        // Pattern 1: Time Distribution
        patterns.add(analyzeTimeDistribution(notes));

        // Pattern 2: Depth vs Fragment
        patterns.add(analyzeDepthVsFragment(notes));

        // Pattern 3: Consistency
        patterns.add(analyzeConsistency(notes, startTime, endTime));

        return patterns;
    }

    /**
     * Analyze time distribution pattern
     */
    private LearningPattern analyzeTimeDistribution(List<Note> notes) {
        Map<String, Integer> timeSlots = new HashMap<>();
        timeSlots.put("早晨 (6-12)", 0);
        timeSlots.put("下午 (12-18)", 0);
        timeSlots.put("晚上 (18-24)", 0);
        timeSlots.put("深夜 (0-6)", 0);

        Map<DayOfWeek, Integer> dayDistribution = new HashMap<>();

        for (Note note : notes) {
            LocalTime time = note.getCreateTime().toLocalTime();
            int hour = time.getHour();

            if (hour >= 6 && hour < 12) {
                timeSlots.put("早晨 (6-12)", timeSlots.get("早晨 (6-12)") + 1);
            } else if (hour >= 12 && hour < 18) {
                timeSlots.put("下午 (12-18)", timeSlots.get("下午 (12-18)") + 1);
            } else if (hour >= 18 && hour < 24) {
                timeSlots.put("晚上 (18-24)", timeSlots.get("晚上 (18-24)") + 1);
            } else {
                timeSlots.put("深夜 (0-6)", timeSlots.get("深夜 (0-6)") + 1);
            }

            DayOfWeek day = note.getCreateTime().getDayOfWeek();
            dayDistribution.put(day, dayDistribution.getOrDefault(day, 0) + 1);
        }

        // Find peak time
        String peakTime = timeSlots.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("未知");

        // Find peak day
        DayOfWeek peakDay = dayDistribution.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        List<String> insights = new ArrayList<>();
        insights.add(String.format("您的学习高峰期在%s", peakTime));
        if (peakDay != null) {
            insights.add(String.format("周%s是您最活跃的学习日", getDayName(peakDay)));
        }

        // Check if learning too late
        int lateNightCount = timeSlots.get("深夜 (0-6)");
        double lateNightPercentage = (double) lateNightCount / notes.size();
        String recommendation;
        if (lateNightPercentage > 0.3) {
            recommendation = "建议调整学习时间，避免深夜学习影响睡眠质量";
        } else if (timeSlots.get("早晨 (6-12)") < notes.size() * 0.1) {
            recommendation = "可以尝试早晨学习，研究表明早晨记忆力更好";
        } else {
            recommendation = "您的学习时间分布较为合理，继续保持";
        }

        LearningPattern pattern = new LearningPattern();
        pattern.setPatternType("TIME_DISTRIBUTION");
        pattern.setDescription("学习时间分布分析");
        pattern.setInsights(insights);
        pattern.setScore(1.0 - lateNightPercentage);  // Lower score if too much late night
        pattern.setRecommendation(recommendation);

        return pattern;
    }

    /**
     * Analyze depth vs fragment pattern
     */
    private LearningPattern analyzeDepthVsFragment(List<Note> notes) {
        int deepNotes = 0;
        int fragmentNotes = 0;

        for (Note note : notes) {
            int contentLength = note.getContent() != null ? note.getContent().length() : 0;
            if (contentLength > 500) {
                deepNotes++;
            } else {
                fragmentNotes++;
            }
        }

        double deepPercentage = (double) deepNotes / notes.size();

        List<String> insights = new ArrayList<>();
        insights.add(String.format("深度笔记占比: %.1f%%", deepPercentage * 100));
        insights.add(String.format("碎片笔记占比: %.1f%%", (1 - deepPercentage) * 100));

        String recommendation;
        if (deepPercentage < 0.3) {
            recommendation = "建议增加深度笔记，系统性学习比碎片化学习更有效";
        } else if (deepPercentage > 0.8) {
            recommendation = "深度笔记很好，也可以适当记录一些快速想法和灵感";
        } else {
            recommendation = "深度笔记和碎片笔记比例合理，保持平衡";
        }

        LearningPattern pattern = new LearningPattern();
        pattern.setPatternType("DEPTH_VS_FRAGMENT");
        pattern.setDescription("学习深度分析");
        pattern.setInsights(insights);
        pattern.setScore(Math.min(deepPercentage * 2, 1.0));  // Prefer deeper notes
        pattern.setRecommendation(recommendation);

        return pattern;
    }

    /**
     * Analyze consistency pattern
     */
    private LearningPattern analyzeConsistency(List<Note> notes, LocalDateTime startTime, LocalDateTime endTime) {
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(startTime, endTime);
        Set<String> activeDays = notes.stream()
                .map(note -> note.getCreateTime().toLocalDate().toString())
                .collect(Collectors.toSet());

        double consistency = (double) activeDays.size() / totalDays;

        List<String> insights = new ArrayList<>();
        insights.add(String.format("学习天数: %d / %d", activeDays.size(), totalDays));
        insights.add(String.format("学习频率: %.1f%%", consistency * 100));

        String recommendation;
        if (consistency < 0.3) {
            recommendation = "建议提高学习频率，每天至少记录一条笔记";
        } else if (consistency > 0.7) {
            recommendation = "学习非常规律，继续保持这个好习惯";
        } else {
            recommendation = "学习频率适中，可以尝试每天固定时间学习";
        }

        LearningPattern pattern = new LearningPattern();
        pattern.setPatternType("CONSISTENCY");
        pattern.setDescription("学习一致性分析");
        pattern.setInsights(insights);
        pattern.setScore(consistency);
        pattern.setRecommendation(recommendation);

        return pattern;
    }

    /**
     * Get day name in Chinese
     */
    private String getDayName(DayOfWeek day) {
        switch (day) {
            case MONDAY: return "一";
            case TUESDAY: return "二";
            case WEDNESDAY: return "三";
            case THURSDAY: return "四";
            case FRIDAY: return "五";
            case SATURDAY: return "六";
            case SUNDAY: return "日";
            default: return "";
        }
    }
}
