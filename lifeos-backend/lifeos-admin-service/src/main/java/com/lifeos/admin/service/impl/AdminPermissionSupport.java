package com.lifeos.admin.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class AdminPermissionSupport {

    private AdminPermissionSupport() {
    }

    static List<String> permissionsForRoles(List<String> roles) {
        Set<String> permissions = new LinkedHashSet<>();
        permissions.add("dashboard:view");
        permissions.add("users:view");
        permissions.add("notes:view");
        permissions.add("tasks:view");
        permissions.add("ai-jobs:view");
        permissions.add("behaviors:view");
        permissions.add("ops:services:view");
        permissions.add("ops:config:view");
        permissions.add("ops:tools:view");

        if (roles.contains("SUPER_ADMIN")) {
            permissions.add("users:toggle");
            permissions.add("users:reset-password");
            permissions.add("notes:delete");
            permissions.add("notes:review");
            permissions.add("tasks:update-status");
            permissions.add("tasks:delete");
            permissions.add("ai-jobs:retry");
            permissions.add("ai-jobs:cancel");
            permissions.add("ops:reset-data");
        }
        if (roles.contains("OPS_ADMIN")) {
            permissions.add("ai-jobs:retry");
            permissions.add("ai-jobs:cancel");
            permissions.add("ops:reset-data");
        }
        return new ArrayList<>(permissions);
    }
}
