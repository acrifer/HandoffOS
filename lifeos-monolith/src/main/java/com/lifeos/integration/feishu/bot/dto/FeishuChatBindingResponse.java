package com.lifeos.integration.feishu.bot.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FeishuChatBindingResponse {
    private Long id;
    private Long userId;
    private String chatId;
    private String chatName;
    private Long skillId;
    private String skillName;
    private Boolean enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
