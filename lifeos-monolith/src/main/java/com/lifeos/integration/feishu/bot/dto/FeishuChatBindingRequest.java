package com.lifeos.integration.feishu.bot.dto;

import lombok.Data;

@Data
public class FeishuChatBindingRequest {
    private String chatId;
    private String chatName;
    private Long skillId;
    private Boolean enabled;
}
