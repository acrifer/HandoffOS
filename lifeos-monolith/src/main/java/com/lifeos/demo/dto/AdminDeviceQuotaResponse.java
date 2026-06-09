package com.lifeos.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AdminDeviceQuotaResponse {
    private String deviceId;
    private Long userId;
    private String deviceName;
    private String displayName;
    private String userAgent;
    private Long quotaLimit;
    private Long quotaUsed;
    private Long remaining;
    private Boolean whitelistEnabled;
    private Boolean enabled;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createTime;
    private List<TokenUsageLogResponse> usageLogs = new ArrayList<>();
}
