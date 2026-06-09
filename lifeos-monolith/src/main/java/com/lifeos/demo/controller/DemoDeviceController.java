package com.lifeos.demo.controller;

import com.lifeos.common.Result;
import com.lifeos.demo.dto.*;
import com.lifeos.demo.exception.ApiException;
import com.lifeos.demo.service.DemoDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DemoDeviceController {

    private final DemoDeviceService demoDeviceService;

    @PostMapping("/auth/device-login")
    public Result<DeviceLoginResponse> deviceLogin(@RequestBody DeviceLoginRequest request) {
        try {
            return Result.success(demoDeviceService.deviceLogin(request));
        } catch (Exception e) {
            return error(e);
        }
    }

    @GetMapping("/quota/me")
    public Result<QuotaStatusResponse> currentQuota() {
        try {
            return Result.success(demoDeviceService.currentQuota());
        } catch (Exception e) {
            return error(e);
        }
    }

    @GetMapping("/admin/quota/devices")
    public Result<List<AdminDeviceQuotaResponse>> listDevices(@RequestHeader(value = "X-Admin-Key", required = false) String adminKey) {
        try {
            return Result.success(demoDeviceService.listDevices(adminKey));
        } catch (Exception e) {
            return error(e);
        }
    }

    @GetMapping("/admin/quota/devices/{deviceId}")
    public Result<AdminDeviceQuotaResponse> getDevice(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @PathVariable String deviceId) {
        try {
            return Result.success(demoDeviceService.getDevice(deviceId, adminKey));
        } catch (Exception e) {
            return error(e);
        }
    }

    @PutMapping("/admin/quota/devices/{deviceId}")
    public Result<AdminDeviceQuotaResponse> updateDevice(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @PathVariable String deviceId,
            @RequestBody UpdateDeviceQuotaRequest request) {
        try {
            return Result.success(demoDeviceService.updateDevice(deviceId, request, adminKey));
        } catch (Exception e) {
            return error(e);
        }
    }

    @PostMapping("/admin/quota/devices/{deviceId}/reset")
    public Result<QuotaStatusResponse> resetDevice(
            @RequestHeader(value = "X-Admin-Key", required = false) String adminKey,
            @PathVariable String deviceId) {
        try {
            return Result.success(demoDeviceService.resetDevice(deviceId, adminKey));
        } catch (Exception e) {
            return error(e);
        }
    }

    private <T> Result<T> error(Exception e) {
        if (e instanceof ApiException apiException) {
            return Result.error(apiException.getCode(), apiException.getMessage());
        }
        return Result.error(e);
    }
}
