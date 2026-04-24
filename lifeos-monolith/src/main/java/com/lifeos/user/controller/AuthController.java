package com.lifeos.user.controller;

import com.lifeos.common.Result;
import com.lifeos.user.dto.LoginRequest;
import com.lifeos.user.dto.LoginResponse;
import com.lifeos.user.dto.RegisterRequest;
import com.lifeos.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * User Authentication Controller
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "User Authentication", description = "User registration and login")
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register new user")
    public Result<LoginResponse> register(@RequestBody RegisterRequest request) {
        try {
            return Result.success(userService.register(request));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/login")
    @Operation(summary = "User login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            return Result.success(userService.login(request));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
