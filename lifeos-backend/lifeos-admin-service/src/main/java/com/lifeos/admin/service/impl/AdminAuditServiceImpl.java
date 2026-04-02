package com.lifeos.admin.service.impl;

import com.lifeos.admin.domain.entity.AdminAuditLog;
import com.lifeos.admin.mapper.AdminAuditLogMapper;
import com.lifeos.admin.service.AdminAuditService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class AdminAuditServiceImpl implements AdminAuditService {

    @Resource
    private AdminAuditLogMapper adminAuditLogMapper;

    @Override
    public void record(Long adminUserId, String adminUsername, String action, String targetType, String targetId,
                       String detail, boolean success, String errorMessage) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdminUserId(adminUserId);
        log.setAdminUsername(adminUsername);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setSuccess(success);
        log.setErrorMessage(errorMessage);
        adminAuditLogMapper.insert(log);
    }
}
