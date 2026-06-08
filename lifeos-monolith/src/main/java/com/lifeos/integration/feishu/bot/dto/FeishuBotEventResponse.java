package com.lifeos.integration.feishu.bot.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FeishuBotEventResponse {
    private Long id;
    private String eventId;
    private String messageId;
    private String chatId;
    private String senderOpenId;
    private String commandType;
    private String requestText;
    private String status;
    private String errorMessage;
    private Long jobId;
    private Long qaLogId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
