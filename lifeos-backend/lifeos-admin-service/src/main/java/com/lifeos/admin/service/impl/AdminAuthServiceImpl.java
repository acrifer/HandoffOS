package com.lifeos.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lifeos.admin.domain.dto.AdminLoginDTO;
import com.lifeos.admin.domain.entity.AdminUser;
import com.lifeos.admin.domain.vo.AdminCurrentUserVO;
import com.lifeos.admin.mapper.AdminUserMapper;
import com.lifeos.admin.service.AdminAuthService;
import com.lifeos.common.utils.JwtUtil;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
/**
 * 管理员独立认证服务。
 *
 * 设计上管理员和普通用户完全分离：
 * - token 使用同一套 JWT 工具
 * - 但管理员 token 会额外带上 ADMIN scope
 * - 并且 Redis 会话存到 admin:token:*，不和普通用户混用
 *
 * 这样网关可以非常明确地区分：
 * - 普通用户只能访问 /api/**
 * - 管理员 token 才能访问 /admin-api/**
 */
public class AdminAuthServiceImpl implements AdminAuthService {

    private static final String TOKEN_KEY_PREFIX = "admin:token:";

    @Resource
    private AdminUserMapper adminUserMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    // ==================== Redis 作用：管理员登录态 ====================
    // 后台管理员的 Redis 作用和普通用户类似，都是“当前最新会话存储”；
    // 但管理员必须独立前缀 admin:token:*，避免后台 token 和前台用户 token 互相污染。
    // ===============================================================

    @Override
    public String login(AdminLoginDTO request) {
        if (request == null || blank(request.getUsername()) || blank(request.getPassword())) {
            throw new RuntimeException("Username and password are required");
        }

        AdminUser adminUser = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, request.getUsername().trim()));
        if (adminUser == null || !passwordEncoder.matches(request.getPassword(), adminUser.getPassword())) {
            throw new RuntimeException("Invalid admin credentials");
        }
        if (Boolean.FALSE.equals(adminUser.getEnabled())) {
            throw new RuntimeException("Admin account has been disabled");
        }

        List<String> roleCodes = adminUserMapper.findRoleCodes(adminUser.getId());
        // 角色编码直接写进 token，
        // 这样网关把角色透传给下游时，不需要再额外查库。
        String token = JwtUtil.generateToken(
                adminUser.getId(),
                adminUser.getUsername(),
                Map.of("scope", "ADMIN", "roleCodes", String.join(",", roleCodes)));
        // ===== Redis 写入：admin:token:* =====
        // 登录成功后把管理员最新 token 写入 Redis。
        // 网关访问 /admin-api/** 时会读取这个 key，只有完全匹配才允许访问后台接口。
        stringRedisTemplate.opsForValue().set(
                TOKEN_KEY_PREFIX + adminUser.getId(),
                token,
                JwtUtil.getExpirationTimeMs(),
                TimeUnit.MILLISECONDS);

        AdminUser update = new AdminUser();
        update.setId(adminUser.getId());
        update.setLastLoginTime(new java.util.Date());
        adminUserMapper.updateById(update);
        return token;
    }

    @Override
    public void logout(Long adminUserId) {
        // ===== Redis 删除：admin:token:* =====
        // 管理员登出本质上也是删除 Redis 中的当前会话记录，
        // 这样旧 JWT 虽然还没到 exp，也会立刻被网关拒绝。
        stringRedisTemplate.delete(TOKEN_KEY_PREFIX + adminUserId);
    }

    @Override
    public AdminCurrentUserVO getCurrentUser(Long adminUserId) {
        AdminUser adminUser = adminUserMapper.selectById(adminUserId);
        if (adminUser == null) {
            throw new RuntimeException("Admin user not found");
        }
        List<String> roles = adminUserMapper.findRoleCodes(adminUserId);
        // 后台前端的菜单和按钮控制直接依赖 permissions，
        // 这里把角色解析成权限集合，一次性返回给前端。
        AdminCurrentUserVO view = new AdminCurrentUserVO();
        view.setId(adminUser.getId());
        view.setUsername(adminUser.getUsername());
        view.setDisplayName(adminUser.getDisplayName());
        view.setEmail(adminUser.getEmail());
        view.setRoles(roles);
        view.setPermissions(AdminPermissionSupport.permissionsForRoles(roles));
        return view;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
