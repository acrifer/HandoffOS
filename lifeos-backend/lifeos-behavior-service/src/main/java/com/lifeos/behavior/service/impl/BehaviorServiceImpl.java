package com.lifeos.behavior.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lifeos.api.behavior.dto.BehaviorEventCommand;
import com.lifeos.api.behavior.dto.BehaviorTrendItemDTO;
import com.lifeos.api.behavior.dto.DashboardRecentNoteDTO;
import com.lifeos.api.behavior.dto.DashboardStatsDTO;
import com.lifeos.api.behavior.dto.DashboardTagStatDTO;
import com.lifeos.behavior.domain.entity.UserBehavior;
import com.lifeos.behavior.domain.projection.BehaviorTrendRow;
import com.lifeos.behavior.domain.projection.DashboardNoteRow;
import com.lifeos.behavior.mapper.BehaviorMapper;
import com.lifeos.behavior.service.BehaviorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
/**
 * 行为统计服务。
 *
 * 这个服务不是直接承载业务操作，而是把各个服务发来的行为事件沉淀下来，
 * 再聚合成 Dashboard 需要的统计结果。
 *
 * 这样做的好处是：
 * - 前端取一个 Dashboard 接口即可
 * - 各业务服务只负责发事件，不负责自己做复杂统计
 * - 统计逻辑集中在一个地方，后续更容易扩展
 */
public class BehaviorServiceImpl extends ServiceImpl<BehaviorMapper, UserBehavior> implements BehaviorService {

    private static final String FINISH_TASK = "FINISH_TASK";
    private static final String ORGANIZE_NOTE = "ORGANIZE_NOTE";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    @Override
    public void recordEvent(BehaviorEventCommand command) {
        if (command == null || command.getUserId() == null || !StringUtils.hasText(command.getActionType())) {
            throw new RuntimeException("Invalid behavior event");
        }

        UserBehavior behavior = new UserBehavior();
        behavior.setEventId(resolveEventId(command.getEventId()));
        behavior.setUserId(command.getUserId());
        behavior.setActionType(command.getActionType().trim().toUpperCase(Locale.ROOT));
        behavior.setTargetId(command.getTargetId());
        try {
            this.save(behavior);
        } catch (DuplicateKeyException ex) {
            // MQ 消费端可能收到重复消息。
            // 这里依赖 eventId 唯一约束实现幂等，重复事件直接忽略即可。
            log.info("Ignored duplicate behavior event {}", behavior.getEventId());
        }
    }

    @Override
    public DashboardStatsDTO getDashboardStats(Long userId) {
        // Dashboard 需要的统计项比较多，这里一次性聚合成单个 DTO 返回给前端，
        // 避免前端自己拼多个接口。
        List<DashboardNoteRow> recentNotes = baseMapper.selectRecentNotes(userId, 60);

        DashboardStatsDTO stats = new DashboardStatsDTO();
        stats.setPendingTaskCount(defaultZero(baseMapper.countPendingTasks(userId)));
        stats.setNoteCount(defaultZero(baseMapper.countNotes(userId)));
        stats.setNotesToReviewCount(defaultZero(baseMapper.countNotesToReview(userId)));
        stats.setWeekNewNoteCount(defaultZero(baseMapper.countNotesCreatedSince(userId, LocalDateTime.now().minusDays(6))));
        stats.setWeekOrganizedNoteCount(defaultZero(baseMapper.countEventsSince(userId, ORGANIZE_NOTE, LocalDateTime.now().minusDays(6))));
        stats.setWeekCompletedTaskCount(defaultZero(baseMapper.countEventsSince(userId, FINISH_TASK, LocalDateTime.now().minusDays(6))));
        stats.setPendingExtractedTaskCount(defaultZero(baseMapper.countPendingExtractedTasks(userId)));
        stats.setAiInboxCount(defaultZero(baseMapper.countAiInbox(userId)));
        stats.setTopTags(buildTopTags(recentNotes));
        stats.setRecentNotes(buildRecentNotes(recentNotes));
        stats.setRecentTrend(buildRecentTrend(userId));
        return stats;
    }

    private List<BehaviorTrendItemDTO> buildRecentTrend(Long userId) {
        LocalDate startDate = LocalDate.now().minusDays(6);
        Map<LocalDate, Long> trendMap = baseMapper.selectRecentTrend(userId, startDate.atStartOfDay()).stream()
                .collect(Collectors.toMap(BehaviorTrendRow::getEventDate, BehaviorTrendRow::getActionCount));

        List<BehaviorTrendItemDTO> trend = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            BehaviorTrendItemDTO item = new BehaviorTrendItemDTO();
            item.setDate(currentDate.format(DATE_FORMATTER));
            item.setCount(trendMap.getOrDefault(currentDate, 0L));
            trend.add(item);
        }
        return trend;
    }

    private Long defaultZero(Long value) {
        return value == null ? 0L : value;
    }

    private String resolveEventId(String eventId) {
        if (StringUtils.hasText(eventId)) {
            return eventId.trim();
        }
        return UUID.randomUUID().toString();
    }

    private List<DashboardTagStatDTO> buildTopTags(List<DashboardNoteRow> recentNotes) {
        return recentNotes.stream()
                .map(DashboardNoteRow::getTags)
                .filter(StringUtils::hasText)
                .flatMap(tags -> List.of(tags.split(",")).stream())
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(6)
                .map(entry -> {
                    DashboardTagStatDTO dto = new DashboardTagStatDTO();
                    dto.setTag(entry.getKey());
                    dto.setCount(entry.getValue());
                    return dto;
                })
                .toList();
    }

    private List<DashboardRecentNoteDTO> buildRecentNotes(List<DashboardNoteRow> recentNotes) {
        return recentNotes.stream()
                .limit(5)
                .map(note -> {
                    DashboardRecentNoteDTO dto = new DashboardRecentNoteDTO();
                    dto.setId(String.valueOf(note.getId()));
                    dto.setTitle(StringUtils.hasText(note.getTitle()) ? note.getTitle() : "Untitled Note");
                    dto.setTags(note.getTags());
                    dto.setReviewState(note.getReviewState());
                    dto.setPinned(Boolean.TRUE.equals(note.getPinned()));
                    dto.setHasSummary(StringUtils.hasText(note.getSummary()));
                    dto.setUpdatedAt(formatDateTime(note.getUpdateTime()));
                    return dto;
                })
                .toList();
    }

    private String formatDateTime(Date value) {
        if (value == null) {
            return "";
        }
        return DATE_TIME_FORMATTER.format(value.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDateTime());
    }
}
