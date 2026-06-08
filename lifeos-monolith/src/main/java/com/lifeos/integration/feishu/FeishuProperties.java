package com.lifeos.integration.feishu;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "feishu")
public class FeishuProperties {

    private String appId;
    private String appSecret;
    private String baseUrl = "https://open.feishu.cn/open-apis";
    private Integer chatHistoryLimit = 80;
    private Boolean demoFallbackEnabled = false;
    private Boolean botEnabled = false;
    private Boolean botLongConnectionEnabled = false;
    private String botVerificationToken = "";
    private String botEncryptKey = "";

    public boolean hasCredentials() {
        return appId != null && !appId.trim().isEmpty() && !"mock".equalsIgnoreCase(appId.trim())
                && appSecret != null && !appSecret.trim().isEmpty() && !"mock".equalsIgnoreCase(appSecret.trim());
    }
}
