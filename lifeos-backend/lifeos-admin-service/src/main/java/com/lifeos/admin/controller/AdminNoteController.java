package com.lifeos.admin.controller;

import com.lifeos.admin.domain.dto.AdminNoteReviewStateDTO;
import com.lifeos.admin.domain.vo.AdminNoteVO;
import com.lifeos.admin.domain.vo.AdminPageResult;
import com.lifeos.admin.service.AdminManagementService;
import com.lifeos.admin.service.impl.AdminAccessSupport;
import com.lifeos.common.response.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/notes")
public class AdminNoteController {

    @Resource
    private AdminManagementService adminManagementService;

    @GetMapping
    public Result<AdminPageResult<AdminNoteVO>> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "reviewState", required = false) String reviewState) {
        try {
            return Result.success(adminManagementService.listNotes(page, size, keyword, reviewState));
        } catch (Exception ex) {
            return Result.error(ex.getMessage());
        }
    }

    @DeleteMapping("/{noteId}")
    public Result<Void> delete(@RequestHeader("X-Admin-Id") Long adminUserId,
                               @RequestHeader("X-Admin-Username") String adminUsername,
                               @RequestHeader("X-Admin-Roles") String adminRoles,
                               @PathVariable Long noteId) {
        try {
            AdminAccessSupport.requireAnyRole(adminRoles, "SUPER_ADMIN");
            adminManagementService.deleteNote(adminUserId, adminUsername, noteId);
            return Result.success();
        } catch (AdminAccessSupport.AdminAccessException ex) {
            return Result.error(403, ex.getMessage());
        } catch (Exception ex) {
            return Result.error(ex.getMessage());
        }
    }

    @PostMapping("/{noteId}/review")
    public Result<Void> updateReview(@RequestHeader("X-Admin-Id") Long adminUserId,
                                     @RequestHeader("X-Admin-Username") String adminUsername,
                                     @RequestHeader("X-Admin-Roles") String adminRoles,
                                     @PathVariable Long noteId,
                                     @RequestBody AdminNoteReviewStateDTO request) {
        try {
            AdminAccessSupport.requireAnyRole(adminRoles, "SUPER_ADMIN");
            adminManagementService.updateNoteReview(adminUserId, adminUsername, noteId, request);
            return Result.success();
        } catch (AdminAccessSupport.AdminAccessException ex) {
            return Result.error(403, ex.getMessage());
        } catch (Exception ex) {
            return Result.error(ex.getMessage());
        }
    }
}
