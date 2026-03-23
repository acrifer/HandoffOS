package com.lifeos.behavior.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifeos.behavior.domain.entity.UserBehavior;
import com.lifeos.behavior.domain.projection.DashboardNoteRow;
import com.lifeos.behavior.domain.projection.BehaviorTrendRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BehaviorMapper extends BaseMapper<UserBehavior> {

    @Select("SELECT COUNT(*) FROM task WHERE user_id = #{userId} AND status <> 2")
    Long countPendingTasks(@Param("userId") Long userId);

    @Select("""
            SELECT
              COALESCE((SELECT COUNT(*) FROM note_0 WHERE user_id = #{userId}), 0) +
              COALESCE((SELECT COUNT(*) FROM note_1 WHERE user_id = #{userId}), 0) +
              COALESCE((SELECT COUNT(*) FROM note_2 WHERE user_id = #{userId}), 0) +
              COALESCE((SELECT COUNT(*) FROM note_3 WHERE user_id = #{userId}), 0)
            """)
    Long countNotes(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM user_behavior
            WHERE user_id = #{userId}
              AND action_type = #{actionType}
              AND create_time >= #{startTime}
            """)
    Long countEventsSince(@Param("userId") Long userId,
            @Param("actionType") String actionType,
            @Param("startTime") LocalDateTime startTime);

    @Select("""
            SELECT DATE(create_time) AS event_date, COUNT(*) AS action_count
            FROM user_behavior
            WHERE user_id = #{userId}
              AND create_time >= #{startTime}
            GROUP BY DATE(create_time)
            ORDER BY event_date
            """)
    List<BehaviorTrendRow> selectRecentTrend(@Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime);

    @Select("""
            SELECT
              COALESCE((SELECT COUNT(*) FROM note_0 WHERE user_id = #{userId} AND review_state IN ('REVIEW', 'EVERGREEN')), 0) +
              COALESCE((SELECT COUNT(*) FROM note_1 WHERE user_id = #{userId} AND review_state IN ('REVIEW', 'EVERGREEN')), 0) +
              COALESCE((SELECT COUNT(*) FROM note_2 WHERE user_id = #{userId} AND review_state IN ('REVIEW', 'EVERGREEN')), 0) +
              COALESCE((SELECT COUNT(*) FROM note_3 WHERE user_id = #{userId} AND review_state IN ('REVIEW', 'EVERGREEN')), 0)
            """)
    Long countNotesToReview(@Param("userId") Long userId);

    @Select("""
            SELECT
              COALESCE((SELECT COUNT(*) FROM note_0 WHERE user_id = #{userId} AND create_time >= #{startTime}), 0) +
              COALESCE((SELECT COUNT(*) FROM note_1 WHERE user_id = #{userId} AND create_time >= #{startTime}), 0) +
              COALESCE((SELECT COUNT(*) FROM note_2 WHERE user_id = #{userId} AND create_time >= #{startTime}), 0) +
              COALESCE((SELECT COUNT(*) FROM note_3 WHERE user_id = #{userId} AND create_time >= #{startTime}), 0)
            """)
    Long countNotesCreatedSince(@Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime);

    @Select("""
            SELECT
              COALESCE((SELECT COUNT(*) FROM note_0 WHERE user_id = #{userId} AND (summary IS NULL OR summary = '' OR tags IS NULL OR tags = '')), 0) +
              COALESCE((SELECT COUNT(*) FROM note_1 WHERE user_id = #{userId} AND (summary IS NULL OR summary = '' OR tags IS NULL OR tags = '')), 0) +
              COALESCE((SELECT COUNT(*) FROM note_2 WHERE user_id = #{userId} AND (summary IS NULL OR summary = '' OR tags IS NULL OR tags = '')), 0) +
              COALESCE((SELECT COUNT(*) FROM note_3 WHERE user_id = #{userId} AND (summary IS NULL OR summary = '' OR tags IS NULL OR tags = '')), 0)
            """)
    Long countAiInbox(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM task
            WHERE user_id = #{userId}
              AND source_note_id IS NOT NULL
              AND status <> 2
            """)
    Long countPendingExtractedTasks(@Param("userId") Long userId);

    @Select("""
            SELECT id, title, tags, summary, review_state, pinned, update_time
            FROM (
                SELECT id, title, tags, summary, review_state, pinned, update_time
                FROM note_0
                WHERE user_id = #{userId}
                UNION ALL
                SELECT id, title, tags, summary, review_state, pinned, update_time
                FROM note_1
                WHERE user_id = #{userId}
                UNION ALL
                SELECT id, title, tags, summary, review_state, pinned, update_time
                FROM note_2
                WHERE user_id = #{userId}
                UNION ALL
                SELECT id, title, tags, summary, review_state, pinned, update_time
                FROM note_3
                WHERE user_id = #{userId}
            ) notes
            ORDER BY pinned DESC, update_time DESC
            LIMIT #{limit}
            """)
    List<DashboardNoteRow> selectRecentNotes(@Param("userId") Long userId,
            @Param("limit") Integer limit);
}
