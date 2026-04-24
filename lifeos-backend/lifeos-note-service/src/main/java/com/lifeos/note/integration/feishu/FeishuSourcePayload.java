package com.lifeos.note.integration.feishu;

import lombok.Data;

import java.util.Date;

@Data
public class FeishuSourcePayload {
    private String sourceType;
    private String externalId;
    private String title;
    private String content;
    private Date sourceTime;
}
