package com.lifeos.integration.feishu.bot.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "feishu_chat_binding")
public class FeishuChatBinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "chat_id", nullable = false, length = 128)
    private String chatId;

    @Column(name = "chat_name")
    private String chatName;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
