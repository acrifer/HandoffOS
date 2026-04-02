package com.lifeos.admin.service;

import com.lifeos.admin.domain.dto.AdminLoginDTO;
import com.lifeos.admin.domain.vo.AdminCurrentUserVO;

public interface AdminAuthService {
    String login(AdminLoginDTO request);

    void logout(Long adminUserId);

    AdminCurrentUserVO getCurrentUser(Long adminUserId);
}
