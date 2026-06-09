package com.lifeos.demo.dto;

import lombok.Data;

@Data
public class UpdateDeviceQuotaRequest {
    private String displayName;
    private Long quotaLimit;
    private Boolean whitelistEnabled;
    private Boolean enabled;
}
