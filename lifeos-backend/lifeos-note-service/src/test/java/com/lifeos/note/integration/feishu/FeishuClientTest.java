package com.lifeos.note.integration.feishu;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeishuClientTest {

    private final FeishuClient feishuClient = new FeishuClient();

    @Test
    void normalizeDocumentIdSupportsUrlAndPlainId() {
        assertThat(feishuClient.normalizeDocumentId("https://example.feishu.cn/docx/ABC123xyz?from=copy"))
                .isEqualTo("ABC123xyz");
        assertThat(feishuClient.normalizeDocumentId("ABC123xyz"))
                .isEqualTo("ABC123xyz");
    }

    @Test
    void contentHashIsStableAndSourceAware() {
        String left = feishuClient.contentHash("FEISHU_DOC", "doc-1", "hello");
        String right = feishuClient.contentHash("FEISHU_DOC", "doc-1", "hello");
        String different = feishuClient.contentHash("FEISHU_CHAT", "doc-1", "hello");

        assertThat(left).isEqualTo(right);
        assertThat(left).isNotEqualTo(different);
    }
}
