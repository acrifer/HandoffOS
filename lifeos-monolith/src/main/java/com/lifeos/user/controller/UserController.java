package com.lifeos.user.controller;

import com.lifeos.common.Result;
import com.lifeos.user.dto.UserInfoResponse;
import com.lifeos.user.entity.User;
import com.lifeos.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/info")
    public Result<UserInfoResponse> info(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            UserInfoResponse response = new UserInfoResponse();
            response.setId(user.getId());
            response.setUsername(user.getUsername());
            response.setEmail(user.getEmail());
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/profile")
    public Result<UserInfoResponse> updateProfile(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestBody UserInfoResponse request) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setEmail(request.getEmail());
            userRepository.save(user);
            UserInfoResponse response = new UserInfoResponse();
            response.setId(user.getId());
            response.setUsername(user.getUsername());
            response.setEmail(user.getEmail());
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
