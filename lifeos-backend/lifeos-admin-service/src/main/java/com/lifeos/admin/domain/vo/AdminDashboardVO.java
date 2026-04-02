package com.lifeos.admin.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class AdminDashboardVO {
    private long totalUsers;
    private long totalNotes;
    private long totalTasks;
    private long failedAiJobs;
    private long onlineServices;
    private long totalServices;
    private List<AdminRecentIssueVO> recentIssues;
}
