package com.lifeos.note.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lifeos.api.ai.dto.AiAsyncJobDTO;
import com.lifeos.api.ai.dto.AiJobType;
import com.lifeos.api.behavior.dto.BehaviorEventCommand;
import com.lifeos.api.behavior.mq.BehaviorMqConstants;
import com.lifeos.note.domain.dto.NoteCreateDTO;
import com.lifeos.note.domain.dto.NotePinDTO;
import com.lifeos.note.domain.dto.NoteReviewDTO;
import com.lifeos.note.domain.dto.NoteSearchQueryDTO;
import com.lifeos.note.domain.dto.NoteUpdateDTO;
import com.lifeos.note.domain.entity.Note;
import com.lifeos.note.domain.entity.NoteReviewState;
import com.lifeos.note.mapper.NoteMapper;
import com.lifeos.note.service.AiJobService;
import com.lifeos.note.service.NoteService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
/**
 * 笔记领域主服务。
 *
 * 这里串起了用户在“知识沉淀”里的核心动作：
 * - 创建/修改/删除笔记
 * - 搜索、置顶、复习状态维护
 * - 触发 AI 总结、整理、任务提取
 * - 发送行为埋点事件
 *
 * 代码分层上有两个关键点：
 * 1. 笔记读写本身是同步事务，尽快给前端返回结果
 * 2. 行为埋点和 AI 处理是异步链路，失败时不阻塞主流程
 */
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements NoteService {

    private static final String SORT_UPDATED = "updated";
    private static final String SORT_CREATED = "created";
    private static final String SORT_REVIEW = "review";
    private static final Set<String> VALID_REVIEW_STATES = Set.of(
            NoteReviewState.NEW,
            NoteReviewState.REVIEW,
            NoteReviewState.EVERGREEN,
            NoteReviewState.ARCHIVED);

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource
    private AiJobService aiJobService;

    @Override
    public Long createNote(Long userId, NoteCreateDTO createDTO) {
        // 创建笔记先落库，再异步记录行为事件。
        // 即使行为事件发送失败，也不能影响用户正常记笔记。
        Note note = new Note();
        note.setUserId(userId);
        note.setTitle(createDTO.getTitle());
        note.setContent(createDTO.getContent());
        note.setTags(createDTO.getTags());
        note.setPinned(Boolean.TRUE.equals(createDTO.getPinned()));
        note.setReviewState(normalizeReviewState(createDTO.getReviewState(), true));
        note.setNextReviewAt(createDTO.getNextReviewAt());

        this.save(note);
        recordBehaviorEvent(userId, "CREATE_NOTE", note.getId());
        return note.getId();
    }

    @Override
    public void updateNote(Long userId, NoteUpdateDTO updateDTO) {
        getOwnedNote(userId, updateDTO.getId());

        LambdaUpdateWrapper<Note> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Note::getId, updateDTO.getId())
                .eq(Note::getUserId, userId);

        // 更新采用“按字段打补丁”的方式：
        // 前端传了什么字段，就更新什么字段，避免把未传字段误覆盖为空。
        boolean hasUpdates = false;
        if (updateDTO.getTitle() != null) {
            wrapper.set(Note::getTitle, updateDTO.getTitle());
            hasUpdates = true;
        }
        if (updateDTO.getContent() != null) {
            wrapper.set(Note::getContent, updateDTO.getContent());
            hasUpdates = true;
        }
        if (updateDTO.getTags() != null) {
            wrapper.set(Note::getTags, updateDTO.getTags());
            hasUpdates = true;
        }
        if (updateDTO.getSummary() != null) {
            wrapper.set(Note::getSummary, updateDTO.getSummary());
            hasUpdates = true;
        }
        if (updateDTO.getPinned() != null) {
            wrapper.set(Note::getPinned, updateDTO.getPinned());
            hasUpdates = true;
        }
        if (updateDTO.getReviewState() != null) {
            wrapper.set(Note::getReviewState, normalizeReviewState(updateDTO.getReviewState(), false));
            hasUpdates = true;
        }
        if (updateDTO.getNextReviewAt() != null || updateDTO.getReviewState() != null) {
            wrapper.set(Note::getNextReviewAt, updateDTO.getNextReviewAt());
            hasUpdates = true;
        }
        if (updateDTO.getLastReviewedAt() != null) {
            wrapper.set(Note::getLastReviewedAt, updateDTO.getLastReviewedAt());
            hasUpdates = true;
        }

        if (!hasUpdates) {
            return;
        }

        if (!this.update(wrapper)) {
            throw new RuntimeException("Note update failed");
        }
        recordBehaviorEvent(userId, "UPDATE_NOTE", updateDTO.getId());
    }

    @Override
    public void deleteNote(Long userId, Long noteId) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getId, noteId).eq(Note::getUserId, userId);
        boolean removed = this.remove(wrapper);
        if (!removed) {
            throw new RuntimeException("Note not found or access denied");
        }
    }

    @Override
    public Note getNoteDetail(Long userId, Long noteId) {
        Note note = getOwnedNote(userId, noteId);
        recordBehaviorEvent(userId, "READ_NOTE", noteId);
        return note;
    }

    @Override
    public List<Note> listUserNotes(Long userId) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getUserId, userId)
                .orderByDesc(Note::getPinned)
                .orderByDesc(Note::getUpdateTime);
        return this.list(wrapper);
    }

    @Override
    public List<Note> searchNotes(Long userId, NoteSearchQueryDTO query) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getUserId, userId);

        if (query != null) {
            // 搜索条件全部下推到数据库，
            // 这样既能利用索引，也能让分片路由继续以 user_id 为主维度裁剪范围。
            applyKeywordFilter(wrapper, query.getKeyword());
            applyTagFilter(wrapper, query.getTags());
            if (query.getPinned() != null) {
                wrapper.eq(Note::getPinned, query.getPinned());
            }
            if (StringUtils.hasText(query.getReviewState())) {
                applyReviewStateFilter(wrapper, query.getReviewState());
            }
            applySummaryFilter(wrapper, query.getHasSummary());
            applyNeedsOrganizationFilter(wrapper, query.getNeedsOrganization());
            applySort(wrapper, query.getSort());
        } else {
            applySort(wrapper, null);
        }

        return this.list(wrapper);
    }

    @Override
    public void updatePin(Long userId, Long noteId, NotePinDTO request) {
        getOwnedNote(userId, noteId);
        LambdaUpdateWrapper<Note> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Note::getId, noteId)
                .eq(Note::getUserId, userId)
                .set(Note::getPinned, request != null && Boolean.TRUE.equals(request.getPinned()));
        if (!this.update(wrapper)) {
            throw new RuntimeException("Note pin update failed");
        }
        recordBehaviorEvent(userId, "PIN_NOTE", noteId);
    }

    @Override
    public void updateReview(Long userId, Long noteId, NoteReviewDTO request) {
        if (request == null) {
            throw new RuntimeException("Review request is required");
        }

        getOwnedNote(userId, noteId);
        LambdaUpdateWrapper<Note> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Note::getId, noteId)
                .eq(Note::getUserId, userId)
                .set(Note::getReviewState, normalizeReviewState(request.getReviewState(), false))
                .set(Note::getNextReviewAt, request.getNextReviewAt())
                .set(Note::getLastReviewedAt, new Date());
        if (!this.update(wrapper)) {
            throw new RuntimeException("Note review update failed");
        }
        recordBehaviorEvent(userId, "REVIEW_NOTE", noteId);
    }

    @Override
    public void updateSummary(Long userId, Long noteId, String summary) {
        LambdaUpdateWrapper<Note> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Note::getId, noteId)
                .eq(Note::getUserId, userId)
                .set(Note::getSummary, summary);
        if (!this.update(wrapper)) {
            throw new RuntimeException("Note summary update failed");
        }
    }

    @Override
    public AiAsyncJobDTO generateSummary(Long userId, Long noteId) {
        // AI 请求统一改造成异步任务：
        // 当前接口只负责创建任务，真正的模型调用在后台 worker 中完成。
        return aiJobService.submitNoteJob(userId, noteId, AiJobType.SUMMARY);
    }

    @Override
    public AiAsyncJobDTO organizeNote(Long userId, Long noteId) {
        return aiJobService.submitNoteJob(userId, noteId, AiJobType.ORGANIZE);
    }

    @Override
    public AiAsyncJobDTO extractTasks(Long userId, Long noteId) {
        return aiJobService.submitNoteJob(userId, noteId, AiJobType.EXTRACT_TASKS);
    }

    private void applyKeywordFilter(LambdaQueryWrapper<Note> wrapper, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }

        wrapper.and(query -> query.like(Note::getTitle, keyword)
                .or()
                .like(Note::getContent, keyword)
                .or()
                .like(Note::getTags, keyword)
                .or()
                .like(Note::getSummary, keyword));
    }

    private void applyTagFilter(LambdaQueryWrapper<Note> wrapper, String tags) {
        if (!StringUtils.hasText(tags)) {
            return;
        }

        String[] normalizedTags = Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
        if (normalizedTags.length == 0) {
            return;
        }

        wrapper.and(query -> {
            for (int index = 0; index < normalizedTags.length; index++) {
                if (index > 0) {
                    query.or();
                }
                query.like(Note::getTags, normalizedTags[index]);
            }
        });
    }

    private void applySummaryFilter(LambdaQueryWrapper<Note> wrapper, Boolean hasSummary) {
        if (hasSummary == null) {
            return;
        }

        if (hasSummary) {
            wrapper.and(query -> query.isNotNull(Note::getSummary).ne(Note::getSummary, ""));
            return;
        }

        wrapper.and(query -> query.isNull(Note::getSummary).or().eq(Note::getSummary, ""));
    }

    private void applyNeedsOrganizationFilter(LambdaQueryWrapper<Note> wrapper, Boolean needsOrganization) {
        if (!Boolean.TRUE.equals(needsOrganization)) {
            return;
        }

        wrapper.and(query -> query.isNull(Note::getSummary)
                .or().eq(Note::getSummary, "")
                .or().isNull(Note::getTags)
                .or().eq(Note::getTags, ""));
    }

    private void applyReviewStateFilter(LambdaQueryWrapper<Note> wrapper, String reviewState) {
        String normalized = reviewState.trim().toUpperCase(Locale.ROOT);
        if ("REVIEWABLE".equals(normalized)) {
            wrapper.in(Note::getReviewState, NoteReviewState.REVIEW, NoteReviewState.EVERGREEN);
            return;
        }
        wrapper.eq(Note::getReviewState, normalizeReviewState(normalized, false));
    }

    private void applySort(LambdaQueryWrapper<Note> wrapper, String sort) {
        String normalizedSort = StringUtils.hasText(sort) ? sort.trim().toLowerCase(Locale.ROOT) : SORT_UPDATED;
        switch (normalizedSort) {
            case SORT_CREATED:
                wrapper.orderByDesc(Note::getPinned)
                        .orderByDesc(Note::getCreateTime);
                break;
            case SORT_REVIEW:
                wrapper.orderByDesc(Note::getPinned)
                        .orderByAsc(Note::getNextReviewAt)
                        .orderByDesc(Note::getUpdateTime);
                break;
            case SORT_UPDATED:
            default:
                wrapper.orderByDesc(Note::getPinned)
                        .orderByDesc(Note::getUpdateTime);
                break;
        }
    }

    private String normalizeReviewState(String reviewState, boolean defaultToNew) {
        if (!StringUtils.hasText(reviewState)) {
            if (defaultToNew) {
                return NoteReviewState.NEW;
            }
            throw new RuntimeException("Review state is required");
        }

        String normalized = reviewState.trim().toUpperCase(Locale.ROOT);
        if (!VALID_REVIEW_STATES.contains(normalized)) {
            throw new RuntimeException("Invalid review state");
        }
        return normalized;
    }

    private void recordBehaviorEvent(Long userId, String actionType, Long targetId) {
        try {
            // 行为埋点只是辅助能力，不能让主业务因为 MQ 抖动而失败。
            BehaviorEventCommand command = new BehaviorEventCommand();
            command.setEventId(UUID.randomUUID().toString());
            command.setUserId(userId);
            command.setActionType(actionType);
            command.setTargetId(targetId);
            rocketMQTemplate.syncSend(BehaviorMqConstants.TOPIC, JSON.toJSONString(command),
                    BehaviorMqConstants.PRODUCER_TIMEOUT_MS);
        } catch (Exception ex) {
            log.warn("Failed to record behavior event {} for user {}", actionType, userId, ex);
        }
    }

    private Note getOwnedNote(Long userId, Long noteId) {
        LambdaQueryWrapper<Note> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Note::getId, noteId).eq(Note::getUserId, userId);
        Note note = this.getOne(wrapper);
        if (note == null) {
            throw new RuntimeException("Note not found or access denied");
        }
        return note;
    }
}
