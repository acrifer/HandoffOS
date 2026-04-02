package com.lifeos.admin.service;

public interface AdminAuditService {
    void record(Long adminUserId, String adminUsername, String action, String targetType, String targetId, String detail,
                boolean success, String errorMessage);
}
