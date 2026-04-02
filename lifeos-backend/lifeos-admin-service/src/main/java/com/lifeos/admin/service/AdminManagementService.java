package com.lifeos.admin.service;

import com.lifeos.admin.domain.dto.AdminNoteReviewStateDTO;
import com.lifeos.admin.domain.vo.AdminAiJobVO;
import com.lifeos.admin.domain.vo.AdminBehaviorVO;
import com.lifeos.admin.domain.vo.AdminNoteVO;
import com.lifeos.admin.domain.vo.AdminPageResult;
import com.lifeos.admin.domain.vo.AdminTaskVO;
import com.lifeos.admin.domain.vo.AdminUserVO;

public interface AdminManagementService {
    AdminPageResult<AdminUserVO> listUsers(int page, int size, String keyword);

    void updateUserEnabled(Long adminUserId, String adminUsername, Long userId, boolean enabled);

    void resetUserPassword(Long adminUserId, String adminUsername, Long userId, String newPassword);

    AdminPageResult<AdminNoteVO> listNotes(int page, int size, String keyword, String reviewState);

    void deleteNote(Long adminUserId, String adminUsername, Long noteId);

    void updateNoteReview(Long adminUserId, String adminUsername, Long noteId, AdminNoteReviewStateDTO request);

    AdminPageResult<AdminTaskVO> listTasks(int page, int size, String keyword, Integer status);

    void updateTaskStatus(Long adminUserId, String adminUsername, Long taskId, Integer status);

    void deleteTask(Long adminUserId, String adminUsername, Long taskId);

    AdminPageResult<AdminAiJobVO> listAiJobs(int page, int size, String keyword, String status, String jobType);

    void retryAiJob(Long adminUserId, String adminUsername, Long jobId);

    void cancelAiJob(Long adminUserId, String adminUsername, Long jobId);

    AdminPageResult<AdminBehaviorVO> listBehaviors(int page, int size, String keyword, String actionType);
}
