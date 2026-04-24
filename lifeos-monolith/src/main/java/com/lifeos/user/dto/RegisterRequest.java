package com.lifeos.user.dto;

import lombok.Data;

/**
 * Register Request DTO
 */
@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
}
