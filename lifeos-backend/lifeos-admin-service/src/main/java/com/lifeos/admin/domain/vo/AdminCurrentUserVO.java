package com.lifeos.admin.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class AdminCurrentUserVO {
    private Long id;
    private String username;
    private String displayName;
    private String email;
    private List<String> roles;
    private List<String> permissions;
}
