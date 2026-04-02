package com.lifeos.admin.controller;

import com.lifeos.admin.domain.vo.AdminConfigItemVO;
import com.lifeos.admin.domain.vo.AdminDashboardVO;
import com.lifeos.admin.domain.vo.AdminServiceStatusVO;
import com.lifeos.admin.domain.vo.AdminToolsVO;
import com.lifeos.admin.service.AdminOpsService;
import com.lifeos.admin.service.impl.AdminAccessSupport;
import com.lifeos.common.response.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/ops")
public class AdminOpsController {

    @Resource
    private AdminOpsService adminOpsService;

    @GetMapping("/dashboard")
    public Result<AdminDashboardVO> dashboard() {
        try {
            return Result.success(adminOpsService.getDashboard());
        } catch (Exception ex) {
            return Result.error(ex.getMessage());
        }
    }

    @GetMapping("/services")
    public Result<List<AdminServiceStatusVO>> services() {
        try {
            return Result.success(adminOpsService.getServices());
        } catch (Exception ex) {
            return Result.error(ex.getMessage());
        }
    }

    @GetMapping("/config")
    public Result<List<AdminConfigItemVO>> config() {
        try {
            return Result.success(adminOpsService.getConfigs());
        } catch (Exception ex) {
            return Result.error(ex.getMessage());
        }
    }

    @GetMapping("/tools")
    public Result<AdminToolsVO> tools() {
        try {
            return Result.success(adminOpsService.getTools());
        } catch (Exception ex) {
            return Result.error(ex.getMessage());
        }
    }

    @PostMapping("/reset-test-data")
    public Result<Void> reset(@RequestHeader("X-Admin-Id") Long adminUserId,
                              @RequestHeader("X-Admin-Username") String adminUsername,
                              @RequestHeader("X-Admin-Roles") String adminRoles) {
        try {
            AdminAccessSupport.requireAnyRole(adminRoles, "SUPER_ADMIN", "OPS_ADMIN");
            adminOpsService.resetTestData(adminUserId, adminUsername);
            return Result.success();
        } catch (AdminAccessSupport.AdminAccessException ex) {
            return Result.error(403, ex.getMessage());
        } catch (Exception ex) {
            return Result.error(ex.getMessage());
        }
    }
}
