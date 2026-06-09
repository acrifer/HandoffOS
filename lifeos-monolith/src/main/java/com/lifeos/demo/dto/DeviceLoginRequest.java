package com.lifeos.demo.dto;

import lombok.Data;

@Data
public class DeviceLoginRequest {
    private String deviceId;
    private String deviceName;
    private String userAgent;
}
