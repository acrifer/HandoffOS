package com.lifeos.integration.feishu.bot.service;

import com.lifeos.integration.feishu.FeishuClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeishuMessageService {

    private static final int MAX_REPLY_LENGTH = 3900;

    private final FeishuClient feishuClient;

    public void replyText(String messageId, String text) {
        String safeText = text == null ? "" : text.trim();
        if (safeText.length() > MAX_REPLY_LENGTH) {
            safeText = safeText.substring(0, MAX_REPLY_LENGTH) + "\n\n内容较长，已截断。完整结果请到 HandoffOS 控制台查看。";
        }
        feishuClient.replyText(messageId, safeText);
    }
}
