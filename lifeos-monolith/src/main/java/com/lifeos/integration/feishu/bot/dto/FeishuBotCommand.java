package com.lifeos.integration.feishu.bot.dto;

import lombok.Data;

@Data
public class FeishuBotCommand {
    private FeishuBotCommandType type = FeishuBotCommandType.UNKNOWN;
    private String originalText;
    private String question;
    private Integer limit;
    private String documentRef;
    private String title;
    private String content;
    private Long taskId;
    private Long qaLogId;
    private String feedbackLabel;
    private String feedbackComment;

    public boolean requiresAsync() {
        return switch (type) {
            case SYNC_RECENT, SYNC_DOCUMENT, DISTILL, MANUAL_DOCUMENT, ASK -> true;
            default -> false;
        };
    }
}
