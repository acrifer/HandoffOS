package com.lifeos.integration.dify;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DifyClientFailFastTest {

    @Test
    void missingApiKeyDoesNotCreateDataset() {
        DifyProperties properties = new DifyProperties();
        properties.setDemoMode(false);
        DifyClient client = new DifyClient(properties);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> client.ensureDataset(null, "支付后端交接助手", "支付回调和上线交接")
        );

        assertTrue(error.getMessage().contains("Dify API key is not configured"));
    }

    @Test
    void demoModeDoesNotReturnDemoAnswer() {
        DifyProperties properties = new DifyProperties();
        properties.setApiKey("real-dataset-key");
        properties.setAskAppKey("real-chat-key");
        properties.setDemoMode(true);
        DifyClient client = new DifyClient(properties);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> client.ensureDataset(null, "支付后端交接助手", "支付回调和上线交接")
        );

        assertTrue(error.getMessage().contains("Dify demo mode is no longer supported"));
    }

    @Test
    void missingWorkflowAndAskKeysFailFast() {
        DifyProperties properties = new DifyProperties();
        properties.setApiKey("real-dataset-key");
        properties.setDemoMode(false);
        DifyClient client = new DifyClient(properties);

        assertThrows(
                IllegalStateException.class,
                () -> client.runDistillWorkflow("支付后端交接助手", "", "dataset-1", List.of(), "user-1")
        );
        assertThrows(
                IllegalStateException.class,
                () -> client.askSkill("支付后端交接助手", "dataset-1", "新人第一天看什么？", "user-1")
        );
    }
}
