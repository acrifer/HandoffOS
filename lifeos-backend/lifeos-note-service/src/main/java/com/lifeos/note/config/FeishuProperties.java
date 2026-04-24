package com.lifeos.note.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "lifeos.feishu")
public class FeishuProperties {
    private String baseUrl = "https://open.feishu.cn/open-apis";
    private String appId;
    private String appSecret;
}
