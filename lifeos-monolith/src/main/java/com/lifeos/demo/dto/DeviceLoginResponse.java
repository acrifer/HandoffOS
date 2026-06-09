package com.lifeos.demo.dto;

import lombok.Data;

@Data
public class DeviceLoginResponse {
    private String token;
    private String deviceId;
    private Long userId;
    private String deviceName;
    private QuotaStatusResponse quota;
}
