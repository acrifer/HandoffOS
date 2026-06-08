package com.lifeos.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Login Response DTO
 */
@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private Long userId;
    private String username;
}
