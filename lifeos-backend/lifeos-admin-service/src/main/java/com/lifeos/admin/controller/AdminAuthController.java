package com.lifeos.admin.controller;

import com.lifeos.admin.domain.dto.AdminLoginDTO;
import com.lifeos.admin.domain.vo.AdminCurrentUserVO;
import com.lifeos.admin.service.AdminAuthService;
import com.lifeos.common.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/auth")
@Tag(name = "管理员认证")
public class AdminAuthController {

    @Resource
    private AdminAuthService adminAuthService;

    @PostMapping("/login")
    @Operation(summary = "管理员登录")
    public Result<String> login(@RequestBody AdminLoginDTO request) {
        try {
            return Result.success(adminAuthService.login(request));
        } catch (Exception ex) {
            return Result.error(ex.getMessage());
        }
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("X-Admin-Id") Long adminUserId) {
        try {
            adminAuthService.logout(adminUserId);
            return Result.success();
        } catch (Exception ex) {
            return Result.error(ex.getMessage());
        }
    }

    @GetMapping("/me")
    public Result<AdminCurrentUserVO> me(@RequestHeader("X-Admin-Id") Long adminUserId) {
        try {
            return Result.success(adminAuthService.getCurrentUser(adminUserId));
        } catch (Exception ex) {
            return Result.error(ex.getMessage());
        }
    }
}
