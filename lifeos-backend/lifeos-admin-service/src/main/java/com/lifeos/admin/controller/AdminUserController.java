package com.lifeos.admin.controller;

import com.lifeos.admin.domain.dto.AdminPasswordResetDTO;
import com.lifeos.admin.domain.vo.AdminPageResult;
import com.lifeos.admin.domain.vo.AdminUserVO;
import com.lifeos.admin.service.AdminManagementService;
import com.lifeos.admin.service.impl.AdminAccessSupport;
import com.lifeos.common.response.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    @Resource
    private AdminManagementService adminManagementService;

    @GetMapping
    public Result<AdminPageResult<AdminUserVO>> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword) {
        try {
            return Result.success(adminManagementService.listUsers(page, size, keyword));
        } catch (Exception ex) {
            return Result.error(ex.getMessage());
        }
    }

    @PostMapping("/{userId}/enable")
    public Result<Void> enable(@RequestHeader("X-Admin-Id") Long adminUserId,
                               @RequestHeader("X-Admin-Username") String adminUsername,
                               @RequestHeader("X-Admin-Roles") String adminRoles,
                               @PathVariable Long userId) {
        try {
            AdminAccessSupport.requireAnyRole(adminRoles, "SUPER_ADMIN");
            adminManagementService.updateUserEnabled(adminUserId, adminUsername, userId, true);
            return Result.success();
        } catch (AdminAccessSupport.AdminAccessException ex) {
            return Result.error(403, ex.getMessage());
        } catch (Exception ex) {
            return Result.error(ex.getMessage());
        }
    }

    @PostMapping("/{userId}/disable")
    public Result<Void> disable(@RequestHeader("X-Admin-Id") Long adminUserId,
                                @RequestHeader("X-Admin-Username") String adminUsername,
                                @RequestHeader("X-Admin-Roles") String adminRoles,
                                @PathVariable Long userId) {
        try {
            AdminAccessSupport.requireAnyRole(adminRoles, "SUPER_ADMIN");
            adminManagementService.updateUserEnabled(adminUserId, adminUsername, userId, false);
            return Result.success();
        } catch (AdminAccessSupport.AdminAccessException ex) {
            return Result.error(403, ex.getMessage());
        } catch (Exception ex) {
            return Result.error(ex.getMessage());
        }
    }

    @PostMapping("/{userId}/reset-password")
    public Result<Void> resetPassword(@RequestHeader("X-Admin-Id") Long adminUserId,
                                      @RequestHeader("X-Admin-Username") String adminUsername,
                                      @RequestHeader("X-Admin-Roles") String adminRoles,
                                      @PathVariable Long userId,
                                      @RequestBody AdminPasswordResetDTO request) {
        try {
            AdminAccessSupport.requireAnyRole(adminRoles, "SUPER_ADMIN");
            adminManagementService.resetUserPassword(adminUserId, adminUsername, userId, request.getNewPassword());
            return Result.success();
        } catch (AdminAccessSupport.AdminAccessException ex) {
            return Result.error(403, ex.getMessage());
        } catch (Exception ex) {
            return Result.error(ex.getMessage());
        }
    }
}
