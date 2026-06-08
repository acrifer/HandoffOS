package com.lifeos.integration.feishu;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FeishuSourceServiceFailFastTest {

    @Test
    void missingCredentialsDoNotReturnDemoSources() {
        FeishuProperties properties = new FeishuProperties();
        properties.setDemoFallbackEnabled(false);
        FeishuClient client = new FeishuClient(properties);
        FeishuSourceService service = new FeishuSourceService(client, properties);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.syncSources(List.of("doc-token"), "chat-id", null, null, 10)
        );

        assertTrue(error.getMessage().contains("Feishu credentials are not configured"));
    }

    @Test
    void emptySourceRequestFailsInsteadOfReturningFixtures() {
        FeishuProperties properties = new FeishuProperties();
        properties.setAppId("cli_real");
        properties.setAppSecret("real-secret");
        FeishuClient client = new FeishuClient(properties);
        FeishuSourceService service = new FeishuSourceService(client, properties);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.syncSources(List.of(), "", null, null, 10)
        );

        assertTrue(error.getMessage().contains("No Feishu source provided"));
    }

    @Test
    void systemMessagePayloadIsIgnoredForKnowledgeSync() throws Exception {
        FeishuProperties properties = new FeishuProperties();
        FeishuClient client = new FeishuClient(properties);

        Method method = FeishuClient.class.getDeclaredMethod("isKnowledgeMessage", String.class, String.class);
        method.setAccessible(true);

        boolean systemMessage = (boolean) method.invoke(
                client,
                "system",
                "{\"template\":\"{from_user} invited {to_chatters} to the group.\"}"
        );
        boolean textMessage = (boolean) method.invoke(
                client,
                "text",
                "发布前先确认回滚脚本和告警联系人"
        );

        assertFalse(systemMessage);
        assertTrue(textMessage);
    }
}
