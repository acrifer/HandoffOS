package com.lifeos.integration.feishu.bot.dto;

import lombok.Data;

@Data
public class FeishuBotStatusResponse {
    private boolean credentialsConfigured;
    private boolean botEnabled;
    private boolean longConnectionEnabled;
    private long bindingCount;
    private long eventCount;
    private long failedEventCount;
}
