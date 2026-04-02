package com.lifeos.admin.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class AdminToolsVO {
    private String frontendUrl;
    private String adminUrl;
    private String gatewayUrl;
    private String swaggerUrl;
    private String nacosUrl;
    private List<String> logCommands;
}
