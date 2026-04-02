package com.lifeos.admin.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lifeos.admin.domain.entity.AdminRole;
import com.lifeos.admin.domain.entity.AdminUser;
import com.lifeos.admin.mapper.AdminRoleMapper;
import com.lifeos.admin.mapper.AdminUserMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    private static final String ROLE_OPS_ADMIN = "OPS_ADMIN";

    @Resource
    private AdminRoleMapper adminRoleMapper;

    @Resource
    private AdminUserMapper adminUserMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        ensureRole(ROLE_SUPER_ADMIN, "超级管理员");
        ensureRole(ROLE_OPS_ADMIN, "运维管理员");
        ensureDefaultAdmin();
    }

    private void ensureRole(String roleCode, String roleName) {
        if (adminRoleMapper.selectCount(new LambdaQueryWrapper<AdminRole>().eq(AdminRole::getRoleCode, roleCode)) == 0) {
            AdminRole role = new AdminRole();
            role.setRoleCode(roleCode);
            role.setRoleName(roleName);
            adminRoleMapper.insert(role);
        }
    }

    private void ensureDefaultAdmin() {
        String username = setting("LIFEOS_ADMIN_DEFAULT_USERNAME", "admin");
        String password = setting("LIFEOS_ADMIN_DEFAULT_PASSWORD", "AdminPass123456");
        String displayName = setting("LIFEOS_ADMIN_DEFAULT_DISPLAY_NAME", "LifeOS Admin");
        String email = setting("LIFEOS_ADMIN_DEFAULT_EMAIL", "admin@lifeos.local");

        if (adminUserMapper.selectCount(new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, username)) > 0) {
            return;
        }

        AdminUser adminUser = new AdminUser();
        adminUser.setUsername(username);
        adminUser.setPassword(passwordEncoder.encode(password));
        adminUser.setDisplayName(displayName);
        adminUser.setEmail(email);
        adminUser.setEnabled(true);
        adminUserMapper.insert(adminUser);
        adminUserMapper.bindRole(adminUser.getId(), findRoleId(ROLE_SUPER_ADMIN));
        log.info("Bootstrapped default admin account '{}'", username);
    }

    private Long findRoleId(String roleCode) {
        return adminRoleMapper.selectList(new LambdaQueryWrapper<AdminRole>().eq(AdminRole::getRoleCode, roleCode))
                .stream()
                .findFirst()
                .map(AdminRole::getId)
                .orElseThrow(() -> new IllegalStateException("Missing role " + roleCode));
    }

    private String setting(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(key);
        }
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
