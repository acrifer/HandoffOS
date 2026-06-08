package com.lifeos.integration.feishu;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeishuSourceItem {

    private String sourceType;
    private String externalId;
    private String title;
    private String content;
    private LocalDateTime sourceTime;
}
