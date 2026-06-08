package com.lifeos.integration.feishu.bot;

import com.lifeos.integration.feishu.bot.dto.FeishuBotCommand;
import com.lifeos.integration.feishu.bot.dto.FeishuBotCommandType;
import com.lifeos.integration.feishu.bot.service.FeishuBotCommandService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeishuBotCommandServiceTest {

    private final FeishuBotCommandService service = new FeishuBotCommandService();

    @Test
    void parsesNaturalQuestionAsAskCommand() {
        FeishuBotCommand command = service.parse("上线前要检查什么？");

        assertEquals(FeishuBotCommandType.ASK, command.getType());
        assertEquals("上线前要检查什么？", command.getQuestion());
        assertTrue(command.requiresAsync());
    }

    @Test
    void parsesSyncRecentCommand() {
        FeishuBotCommand command = service.parse("/同步 最近20条");

        assertEquals(FeishuBotCommandType.SYNC_RECENT, command.getType());
        assertEquals(20, command.getLimit());
    }

    @Test
    void parsesSyncDocumentCommand() {
        FeishuBotCommand command = service.parse("/同步 文档 https://example.feishu.cn/wiki/abc");

        assertEquals(FeishuBotCommandType.SYNC_DOCUMENT, command.getType());
        assertEquals("https://example.feishu.cn/wiki/abc", command.getDocumentRef());
    }

    @Test
    void parsesManualDocumentCommand() {
        FeishuBotCommand command = service.parse("/资料 发布检查 | 灰度、回滚和值班负责人必须确认。");

        assertEquals(FeishuBotCommandType.MANUAL_DOCUMENT, command.getType());
        assertEquals("发布检查", command.getTitle());
        assertEquals("灰度、回滚和值班负责人必须确认。", command.getContent());
    }

    @Test
    void parsesTaskAndFeedbackCommands() {
        FeishuBotCommand task = service.parse("/任务 完成 42");
        FeishuBotCommand feedback = service.parse("/反馈 9 缺口 需要补发布手册");

        assertEquals(FeishuBotCommandType.TASK_COMPLETE, task.getType());
        assertEquals(42L, task.getTaskId());
        assertEquals(FeishuBotCommandType.FEEDBACK, feedback.getType());
        assertEquals(9L, feedback.getQaLogId());
        assertEquals("缺口", feedback.getFeedbackLabel());
        assertEquals("需要补发布手册", feedback.getFeedbackComment());
    }
}
