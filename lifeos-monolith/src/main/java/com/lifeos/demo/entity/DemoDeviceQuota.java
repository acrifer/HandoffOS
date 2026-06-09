package com.lifeos.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "demo_device_quota")
public class DemoDeviceQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, unique = true, length = 120)
    private String deviceId;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "quota_limit", nullable = false)
    private Long quotaLimit = 100000L;

    @Column(name = "quota_used", nullable = false)
    private Long quotaUsed = 0L;

    @Column(name = "whitelist_enabled", nullable = false)
    private Boolean whitelistEnabled = false;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "period_start")
    private LocalDateTime periodStart;

    @Column(name = "period_end")
    private LocalDateTime periodEnd;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createTime = now;
        updateTime = now;
        if (periodStart == null) {
            periodStart = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
