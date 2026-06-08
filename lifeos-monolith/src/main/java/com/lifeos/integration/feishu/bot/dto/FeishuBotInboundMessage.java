package com.lifeos.integration.feishu.bot.dto;

import lombok.Data;

@Data
public class FeishuBotInboundMessage {
    private String eventId;
    private String messageId;
    private String chatId;
    private String senderOpenId;
    private String requestText;
    private String messageType;
    private String rawPayload;
}
