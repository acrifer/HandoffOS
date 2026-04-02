package com.lifeos.admin.service;

import com.lifeos.admin.domain.vo.AdminConfigItemVO;
import com.lifeos.admin.domain.vo.AdminDashboardVO;
import com.lifeos.admin.domain.vo.AdminServiceStatusVO;
import com.lifeos.admin.domain.vo.AdminToolsVO;

import java.util.List;

public interface AdminOpsService {
    AdminDashboardVO getDashboard();

    List<AdminServiceStatusVO> getServices();

    List<AdminConfigItemVO> getConfigs();

    AdminToolsVO getTools();

    void resetTestData(Long adminUserId, String adminUsername);
}
