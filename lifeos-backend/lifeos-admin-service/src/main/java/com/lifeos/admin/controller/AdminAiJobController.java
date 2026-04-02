package com.lifeos.admin.controller;

import com.lifeos.admin.domain.vo.AdminAiJobVO;
import com.lifeos.admin.domain.vo.AdminPageResult;
import com.lifeos.admin.service.AdminManagementService;
import com.lifeos.admin.service.impl.AdminAccessSupport;
import com.lifeos.common.response.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/ai-jobs")
public class AdminAiJobController {

    @Resource
    private AdminManagementService adminManagementService;

    @GetMapping
    public Result<AdminPageResult<AdminAiJobVO>> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "jobType", required = false) String jobType) {
        try {
            return Result.success(adminManagementService.listAiJobs(page, size, keyword, status, jobType));
        } catch (Exception ex) {
            return Result.error(ex.getMessage());
        }
    }

    @PostMapping("/{jobId}/retry")
    public Result<Void> retry(@RequestHeader("X-Admin-Id") Long adminUserId,
                              @RequestHeader("X-Admin-Username") String adminUsername,
                              @RequestHeader("X-Admin-Roles") String adminRoles,
                              @PathVariable Long jobId) {
        try {
            AdminAccessSupport.requireAnyRole(adminRoles, "SUPER_ADMIN", "OPS_ADMIN");
            adminManagementService.retryAiJob(adminUserId, adminUsername, jobId);
            return Result.success();
        } catch (AdminAccessSupport.AdminAccessException ex) {
            return Result.error(403, ex.getMessage());
        } catch (Exception ex) {
            return Result.error(ex.getMessage());
        }
    }

    @PostMapping("/{jobId}/cancel")
    public Result<Void> cancel(@RequestHeader("X-Admin-Id") Long adminUserId,
                               @RequestHeader("X-Admin-Username") String adminUsername,
                               @RequestHeader("X-Admin-Roles") String adminRoles,
                               @PathVariable Long jobId) {
        try {
            AdminAccessSupport.requireAnyRole(adminRoles, "SUPER_ADMIN", "OPS_ADMIN");
            adminManagementService.cancelAiJob(adminUserId, adminUsername, jobId);
            return Result.success();
        } catch (AdminAccessSupport.AdminAccessException ex) {
            return Result.error(403, ex.getMessage());
        } catch (Exception ex) {
            return Result.error(ex.getMessage());
        }
    }
}
