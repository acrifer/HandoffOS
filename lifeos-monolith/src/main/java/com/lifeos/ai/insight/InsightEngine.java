package com.lifeos.ai.insight;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.config.AiProperties;
import com.lifeos.note.entity.Note;
import com.lifeos.note.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Insight Engine
 * Generates comprehensive learning insights and recommendations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InsightEngine {

    private final NoteRepository noteRepository;
    private final LearningPatternAnalyzer patternAnalyzer;
    private final AiProperties aiProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generate weekly insight report
     */
    public InsightReport generateWeeklyReport(Long userId) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusWeeks(1);

        return generateReport(userId, startTime, endTime, "本周");
    }

    /**
     * Generate monthly insight report
     */
    public InsightReport generateMonthlyReport(Long userId) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusMonths(1);

        return generateReport(userId, startTime, endTime, "本月");
    }

    /**
     * Generate insight report for a period
     */
    private InsightReport generateReport(Long userId, LocalDateTime startTime, LocalDateTime endTime, String period) {
        log.info("Generating {} insight report for user {}", period, userId);

        // Get notes in period
        List<Note> notes = noteRepository.findByUserId(userId).stream()
                .filter(note -> note.getCreateTime().isAfter(startTime) && note.getCreateTime().isBefore(endTime))
                .collect(Collectors.toList());

        // Basic statistics
        Map<String, Integer> statistics = new HashMap<>();
        statistics.put("totalNotes", notes.size());
        statistics.put("totalWords", notes.stream()
                .mapToInt(note -> note.getContent() != null ? note.getContent().length() : 0)
                .sum());
        statistics.put("avgWordsPerNote", notes.isEmpty() ? 0 :
                statistics.get("totalWords") / notes.size());

        // Topic clustering
        List<TopicCluster> topicClusters = clusterByTopics(notes);

        // Learning patterns
        List<LearningPattern> patterns = patternAnalyzer.analyzePatterns(userId, startTime, endTime);

        // Personalized recommendations
        List<String> recommendations = generateRecommendations(notes, topicClusters, patterns);

        // Overall score
        double overallScore = calculateOverallScore(patterns);

        // AI-generated summary
        String summary = generateSummary(userId, period, statistics, topicClusters, patterns);

        InsightReport report = new InsightReport();
        report.setPeriod(period);
        report.setStatistics(statistics);
        report.setTopicClusters(topicClusters);
        report.setPatterns(patterns);
        report.setRecommendations(recommendations);
        report.setOverallScore(overallScore);
        report.setSummary(summary);

        return report;
    }

    /**
     * Cluster notes by topics using tags and keywords
     */
    private List<TopicCluster> clusterByTopics(List<Note> notes) {
        Map<String, List<Note>> topicMap = new HashMap<>();

        for (Note note : notes) {
            String tags = note.getTags();
            if (tags != null && !tags.isEmpty()) {
                String[] tagArray = tags.split(",");
                for (String tag : tagArray) {
                    tag = tag.trim();
                    topicMap.computeIfAbsent(tag, k -> new ArrayList<>()).add(note);
                }
            } else {
                // No tags, use "未分类"
                topicMap.computeIfAbsent("未分类", k -> new ArrayList<>()).add(note);
            }
        }

        List<TopicCluster> clusters = new ArrayList<>();
        int totalNotes = notes.size();

        for (Map.Entry<String, List<Note>> entry : topicMap.entrySet()) {
            String topic = entry.getKey();
            List<Note> topicNotes = entry.getValue();

            TopicCluster cluster = new TopicCluster();
            cluster.setTopicName(topic);
            cluster.setNoteIds(topicNotes.stream().map(Note::getId).collect(Collectors.toList()));
            cluster.setNoteCount(topicNotes.size());
            cluster.setPercentage((double) topicNotes.size() / totalNotes);
            cluster.setKeywords(extractKeywords(topicNotes));
            cluster.setTrend("STABLE");  // Simplified, could compare with previous period

            clusters.add(cluster);
        }

        // Sort by note count descending
        clusters.sort((a, b) -> b.getNoteCount().compareTo(a.getNoteCount()));

        return clusters;
    }

    /**
     * Extract keywords from notes
     */
    private List<String> extractKeywords(List<Note> notes) {
        Map<String, Integer> wordFreq = new HashMap<>();

        for (Note note : notes) {
            if (note.getTitle() != null) {
                String[] words = note.getTitle().split("\\s+");
                for (String word : words) {
                    if (word.length() > 1) {
                        wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
                    }
                }
            }
        }

        return wordFreq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Generate personalized recommendations
     */
    private List<String> generateRecommendations(List<Note> notes,
                                                  List<TopicCluster> clusters,
                                                  List<LearningPattern> patterns) {
        List<String> recommendations = new ArrayList<>();

        // Recommendation 1: Based on topic distribution
        if (!clusters.isEmpty()) {
            TopicCluster topCluster = clusters.get(0);
            if (topCluster.getPercentage() > 0.6) {
                recommendations.add(String.format(
                        "您%s的笔记集中在「%s」，建议拓展其他领域的学习",
                        topCluster.getPercentage() > 0.8 ? "80%" : "60%以上",
                        topCluster.getTopicName()
                ));
            }
        }

        // Recommendation 2: Based on patterns
        for (LearningPattern pattern : patterns) {
            if (pattern.getScore() < 0.5) {
                recommendations.add(pattern.getRecommendation());
            }
        }

        // Recommendation 3: Check for gaps
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysAgo = now.minusDays(3);
        long recentNotes = notes.stream()
                .filter(note -> note.getCreateTime().isAfter(threeDaysAgo))
                .count();

        if (recentNotes == 0) {
            recommendations.add("您已经3天没有记录笔记了，建议恢复学习节奏");
        }

        // Recommendation 4: Encourage deep learning
        long deepNotes = notes.stream()
                .filter(note -> note.getContent() != null && note.getContent().length() > 500)
                .count();

        if (deepNotes < notes.size() * 0.3) {
            recommendations.add("建议增加深度笔记，系统性学习比碎片化学习更有效");
        }

        return recommendations;
    }

    /**
     * Calculate overall learning score
     */
    private double calculateOverallScore(List<LearningPattern> patterns) {
        if (patterns.isEmpty()) {
            return 0.5;
        }

        double totalScore = patterns.stream()
                .mapToDouble(LearningPattern::getScore)
                .sum();

        return totalScore / patterns.size();
    }

    /**
     * Generate AI summary
     */
    private String generateSummary(Long userId, String period,
                                    Map<String, Integer> statistics,
                                    List<TopicCluster> clusters,
                                    List<LearningPattern> patterns) {
        if (!aiProperties.hasApiKey()) {
            return generateMockSummary(period, statistics, clusters, patterns);
        }

        try {
            String prompt = buildSummaryPrompt(period, statistics, clusters, patterns);
            return callLLM(prompt);

        } catch (Exception e) {
            log.error("Failed to generate summary via LLM: {}", e.getMessage());
            return generateMockSummary(period, statistics, clusters, patterns);
        }
    }

    /**
     * Build prompt for summary generation
     */
    private String buildSummaryPrompt(String period,
                                       Map<String, Integer> statistics,
                                       List<TopicCluster> clusters,
                                       List<LearningPattern> patterns) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format("请为用户生成%s的学习总结。\n\n", period));

        prompt.append("统计数据：\n");
        prompt.append(String.format("- 笔记数量: %d\n", statistics.get("totalNotes")));
        prompt.append(String.format("- 总字数: %d\n", statistics.get("totalWords")));
        prompt.append(String.format("- 平均每篇: %d 字\n\n", statistics.get("avgWordsPerNote")));

        prompt.append("主题分布：\n");
        for (int i = 0; i < Math.min(3, clusters.size()); i++) {
            TopicCluster cluster = clusters.get(i);
            prompt.append(String.format("- %s: %d 篇 (%.1f%%)\n",
                    cluster.getTopicName(),
                    cluster.getNoteCount(),
                    cluster.getPercentage() * 100));
        }
        prompt.append("\n");

        prompt.append("学习模式：\n");
        for (LearningPattern pattern : patterns) {
            prompt.append(String.format("- %s: %.1f 分\n",
                    pattern.getDescription(),
                    pattern.getScore() * 100));
        }

        prompt.append("\n请生成一段简洁的总结（100字以内），包括：\n");
        prompt.append("1. 本期学习的主要成果\n");
        prompt.append("2. 学习习惯的评价\n");
        prompt.append("3. 一句鼓励的话");

        return prompt.toString();
    }

    /**
     * Call LLM for summary generation
     */
    private String callLLM(String prompt) throws Exception {
        String url = aiProperties.getBaseUrl() + "/chat/completions";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", aiProperties.getModel());
        requestBody.put("messages", Arrays.asList(
                Map.of("role", "system", "content", "你是一个学习分析助手，擅长生成简洁有洞察力的学习总结。"),
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 500);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(aiProperties.getApiKey());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        String response = restTemplate.postForObject(url, request, String.class);

        JsonNode root = objectMapper.readTree(response);
        return root.path("choices").get(0).path("message").path("content").asText();
    }

    /**
     * Generate mock summary for development
     */
    private String generateMockSummary(String period,
                                        Map<String, Integer> statistics,
                                        List<TopicCluster> clusters,
                                        List<LearningPattern> patterns) {
        StringBuilder summary = new StringBuilder();

        summary.append(String.format("%s您共记录了 %d 篇笔记，总计 %d 字。",
                period,
                statistics.get("totalNotes"),
                statistics.get("totalWords")));

        if (!clusters.isEmpty()) {
            TopicCluster topCluster = clusters.get(0);
            summary.append(String.format("学习重点集中在「%s」领域。", topCluster.getTopicName()));
        }

        double avgScore = patterns.stream()
                .mapToDouble(LearningPattern::getScore)
                .average()
                .orElse(0.5);

        if (avgScore > 0.7) {
            summary.append("学习习惯良好，继续保持！");
        } else if (avgScore > 0.5) {
            summary.append("学习习惯尚可，还有提升空间。");
        } else {
            summary.append("建议调整学习习惯，提高学习效率。");
        }

        return summary.toString();
    }
}
