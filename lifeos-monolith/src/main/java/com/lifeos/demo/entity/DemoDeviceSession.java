package com.lifeos.demo.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "demo_device_session")
public class DemoDeviceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false, unique = true, length = 120)
    private String deviceId;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createTime = now;
        updateTime = now;
        lastSeenAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
