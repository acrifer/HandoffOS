package com.lifeos.admin.domain.vo;

import lombok.Data;

@Data
public class AdminServiceStatusVO {
    private String name;
    private String category;
    private String host;
    private int port;
    private String status;
    private String accessUrl;
    private String detail;
}
