package com.lifeos.admin.domain.vo;

import lombok.Data;

@Data
public class AdminConfigItemVO {
    private String key;
    private String value;
    private Boolean masked;
}
