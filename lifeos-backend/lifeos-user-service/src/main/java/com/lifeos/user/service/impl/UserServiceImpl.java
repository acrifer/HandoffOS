package com.lifeos.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lifeos.common.utils.JwtUtil;
import com.lifeos.user.domain.dto.LoginDTO;
import com.lifeos.user.domain.dto.RegisterDTO;
import com.lifeos.user.domain.dto.UserPasswordUpdateDTO;
import com.lifeos.user.domain.dto.UserProfileUpdateDTO;
import com.lifeos.user.domain.entity.User;
import com.lifeos.user.mapper.UserMapper;
import com.lifeos.user.service.UserService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Service
/**
 * 用户账户主流程实现。
 *
 * 这个类负责三件核心事情：
 * 1. 注册：写入用户基础资料
 * 2. 登录：校验用户名密码，并把当前有效 token 写入 Redis
 * 3. 资料/密码更新：更新成功后重新签发 token，确保会话里的用户名等信息与数据库一致
 *
 * 这里的设计重点不是“生成 JWT”本身，而是“让 JWT 和 Redis 会话一起工作”：
 * - JWT 里存用户身份
 * - Redis 里只保留当前用户最新的一份 token
 * 所以用户重新登录、改密码、退出登录后，旧 token 都可以立即失效。
 */
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String TOKEN_KEY_PREFIX = "token:";
    private static final String LOGIN_LIMIT_KEY_PREFIX = "login:limit:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    // ==================== Redis 作用：用户认证与限流 ====================
    // 1. token:*         保存“当前最新登录 token”，用于网关鉴权和主动失效
    // 2. login:limit:*   保存登录尝试次数，做轻量限流
    // 它不是用户资料缓存层，用户详情仍然以数据库为准。
    // ================================================================

    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public void register(RegisterDTO registerDTO) {
        // 注册阶段先做用户名唯一性校验，避免写入后再回滚。
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, registerDTO.getUsername());
        long count = this.count(wrapper);
        if (count > 0) {
            throw new RuntimeException("Username already exists");
        }

        // 用户密码只保存加密后的结果，数据库中不存明文密码。
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        user.setEnabled(true);

        this.save(user);
    }

    @Override
    public String login(LoginDTO loginDTO) {
        String username = loginDTO.getUsername();
        // ===== Redis 计数：login:limit:* =====
        // 使用 Redis 做一个轻量限流：
        // 同一用户名 1 分钟内最多尝试 5 次，防止暴力撞库。
        String limitKey = LOGIN_LIMIT_KEY_PREFIX + username;
        Long attempts = stringRedisTemplate.opsForValue().increment(limitKey);
        if (attempts != null && attempts == 1) {
            stringRedisTemplate.expire(limitKey, 1, TimeUnit.MINUTES);
        }
        if (attempts != null && attempts > 5) {
            throw new RuntimeException("Too many login attempts. Please try again later.");
        }

        // 登录只按用户名查找，密码校验统一交给 isPasswordValid。
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);

        User user = this.getOne(wrapper);
        if (user == null || !isPasswordValid(user, loginDTO.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }
        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new RuntimeException("User account has been disabled");
        }

        // ===== Redis 删除：login:limit:* =====
        // 登录成功后清掉限流计数，避免后续正常登录也被限流影响。
        // 这里保留 Redis 的好处是：失败计数天然带过期时间，不需要额外清理表。
        stringRedisTemplate.delete(limitKey);

        return rotateToken(user);
    }

    @Override
    public User getCurrentUserInfo(Long userId) {
        User user = this.getById(userId);
        if (user != null) {
            // 对外返回用户资料时，统一去掉密码字段。
            user.setPassword(null);
        }
        return user;
    }

    @Override
    public String updateProfile(Long userId, UserProfileUpdateDTO updateDTO) {
        if (updateDTO == null) {
            throw new RuntimeException("Update request is required");
        }

        User user = getRequiredUser(userId);
        String username = normalizeRequiredText(updateDTO.getUsername(), "Username is required");
        String email = normalizeOptionalText(updateDTO.getEmail());

        validateUsernameAvailable(userId, username);

        user.setUsername(username);
        user.setEmail(email);
        if (!this.updateById(user)) {
            throw new RuntimeException("Failed to update profile");
        }

        return rotateToken(user);
    }

    @Override
    public String updatePassword(Long userId, UserPasswordUpdateDTO updateDTO) {
        if (updateDTO == null) {
            throw new RuntimeException("Password update request is required");
        }

        String currentPassword = normalizeRequiredText(updateDTO.getCurrentPassword(), "Current password is required");
        String newPassword = normalizeRequiredText(updateDTO.getNewPassword(), "New password is required");
        if (newPassword.length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters");
        }

        User user = getRequiredUser(userId);
        if (!isPasswordValid(user, currentPassword)) {
            throw new RuntimeException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        if (!this.updateById(user)) {
            throw new RuntimeException("Failed to update password");
        }

        return rotateToken(user);
    }

    @Override
    public void logout(Long userId) {
        // ===== Redis 删除：token:* =====
        // 登出时只需要删除 Redis 中当前用户的 token key。
        // JWT 本身无法被“远程撤销”，所以真正让会话立即失效的动作在这里。
        stringRedisTemplate.delete(TOKEN_KEY_PREFIX + userId);
    }

    private boolean isPasswordValid(User user, String rawPassword) {
        String storedPassword = user.getPassword();
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        // 兼容历史测试数据：
        // 旧种子数据里可能仍是明文密码，这里允许登录一次，并立即升级成 bcrypt。
        if (isBcryptHash(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }

        if (!storedPassword.equals(rawPassword)) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(rawPassword));
        this.updateById(user);
        return true;
    }

    private boolean isBcryptHash(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }

    private User getRequiredUser(Long userId) {
        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        return user;
    }

    private void validateUsernameAvailable(Long userId, String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username).ne(User::getId, userId);
        if (this.count(wrapper) > 0) {
            throw new RuntimeException("Username already exists");
        }
    }

    private String rotateToken(User user) {
        // ===== Redis 写入：token:* =====
        // 每次登录、改资料、改密码后都重新签发 token，并覆盖 Redis 中的旧值。
        // 这里 Redis 的核心价值是实现“单用户只认最新一份 token”：
        // - 新登录会顶掉旧登录
        // - 更新资料后，新 token 会携带新的用户名等声明
        // - 改密码后，旧 token 也会被一并作废
        //
        // 网关后续读取 token:* 时，只要发现请求携带的 JWT 不是 Redis 里这份，
        // 就会直接判定为“过期或已退出”。
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());
        stringRedisTemplate.opsForValue().set(
                TOKEN_KEY_PREFIX + user.getId(),
                token,
                JwtUtil.getExpirationTimeMs(),
                TimeUnit.MILLISECONDS);
        return token;
    }

    private String normalizeRequiredText(String value, String message) {
        String normalized = normalizeOptionalText(value);
        if (normalized == null) {
            throw new RuntimeException(message);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
