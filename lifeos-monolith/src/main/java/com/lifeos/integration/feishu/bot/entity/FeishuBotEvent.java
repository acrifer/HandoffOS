package com.lifeos.integration.feishu.bot.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "feishu_bot_event")
public class FeishuBotEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 128)
    private String eventId;

    @Column(name = "message_id", length = 128)
    private String messageId;

    @Column(name = "chat_id", nullable = false, length = 128)
    private String chatId;

    @Column(name = "sender_open_id", length = 128)
    private String senderOpenId;

    @Column(name = "command_type", length = 40)
    private String commandType;

    @Column(name = "request_text", columnDefinition = "TEXT")
    private String requestText;

    @Column(nullable = false, length = 32)
    private String status = "RECEIVED";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "job_id")
    private Long jobId;

    @Column(name = "qa_log_id")
    private Long qaLogId;

    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

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
