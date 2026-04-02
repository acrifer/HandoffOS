package com.lifeos.admin.controller;

import com.lifeos.admin.domain.vo.AdminBehaviorVO;
import com.lifeos.admin.domain.vo.AdminPageResult;
import com.lifeos.admin.service.AdminManagementService;
import com.lifeos.common.response.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/behaviors")
public class AdminBehaviorController {

    @Resource
    private AdminManagementService adminManagementService;

    @GetMapping
    public Result<AdminPageResult<AdminBehaviorVO>> list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "actionType", required = false) String actionType) {
        try {
            return Result.success(adminManagementService.listBehaviors(page, size, keyword, actionType));
        } catch (Exception ex) {
            return Result.error(ex.getMessage());
        }
    }
}
