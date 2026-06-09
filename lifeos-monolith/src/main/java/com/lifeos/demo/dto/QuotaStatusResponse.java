package com.lifeos.demo.dto;

import lombok.Data;

@Data
public class QuotaStatusResponse {
    private String deviceId;
    private Long limit;
    private Long used;
    private Long remaining;
    private Boolean whitelisted;
    private Boolean enabled;
    private Boolean unlimited;
}
