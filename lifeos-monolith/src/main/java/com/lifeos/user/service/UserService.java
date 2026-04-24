package com.lifeos.user.service;

import com.lifeos.config.JwtTokenUtil;
import com.lifeos.user.dto.LoginRequest;
import com.lifeos.user.dto.LoginResponse;
import com.lifeos.user.dto.RegisterRequest;
import com.lifeos.user.entity.User;
import com.lifeos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User Service
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setEnabled(true);

        user = userRepository.save(user);

        String token = jwtTokenUtil.generateToken(user.getId(), user.getUsername());
        return new LoginResponse(token, user.getId(), user.getUsername());
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        if (!user.getEnabled()) {
            throw new RuntimeException("User account is disabled");
        }

        String token = jwtTokenUtil.generateToken(user.getId(), user.getUsername());
        return new LoginResponse(token, user.getId(), user.getUsername());
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
