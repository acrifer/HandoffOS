package com.lifeos.admin.controller;

import com.lifeos.admin.domain.dto.AdminTaskStatusDTO;
import com.lifeos.admin.domain.vo.AdminPageResult;
import com.lifeos.admin.domain.vo.AdminTaskVO;
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
@RequestMapping("/admin/tasks")
public class AdminTaskController {

    @Resource
    private AdminManagementService adminManagementService;

    @GetMapping
    public Result<AdminPageResult<AdminTaskVO>> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) Integer status) {
        try {
            return Result.success(adminManagementService.listTasks(page, size, keyword, status));
        } catch (Exception ex) {
            return Result.error(ex.getMessage());
        }
    }

    @PostMapping("/{taskId}/status")
    public Result<Void> updateStatus(@RequestHeader("X-Admin-Id") Long adminUserId,
                                     @RequestHeader("X-Admin-Username") String adminUsername,
                                     @RequestHeader("X-Admin-Roles") String adminRoles,
                                     @PathVariable Long taskId,
                                     @RequestBody AdminTaskStatusDTO request) {
        try {
            AdminAccessSupport.requireAnyRole(adminRoles, "SUPER_ADMIN");
            adminManagementService.updateTaskStatus(adminUserId, adminUsername, taskId, request.getStatus());
            return Result.success();
        } catch (AdminAccessSupport.AdminAccessException ex) {
            return Result.error(403, ex.getMessage());
        } catch (Exception ex) {
            return Result.error(ex.getMessage());
        }
    }

    @DeleteMapping("/{taskId}")
    public Result<Void> delete(@RequestHeader("X-Admin-Id") Long adminUserId,
                               @RequestHeader("X-Admin-Username") String adminUsername,
                               @RequestHeader("X-Admin-Roles") String adminRoles,
                               @PathVariable Long taskId) {
        try {
            AdminAccessSupport.requireAnyRole(adminRoles, "SUPER_ADMIN");
            adminManagementService.deleteTask(adminUserId, adminUsername, taskId);
            return Result.success();
        } catch (AdminAccessSupport.AdminAccessException ex) {
            return Result.error(403, ex.getMessage());
        } catch (Exception ex) {
            return Result.error(ex.getMessage());
        }
    }
}
