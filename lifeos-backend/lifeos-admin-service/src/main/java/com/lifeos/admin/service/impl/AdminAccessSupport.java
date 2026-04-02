package com.lifeos.admin.service.impl;

import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class AdminAccessSupport {

    private AdminAccessSupport() {
    }

    public static void requireAnyRole(String headerValue, String... allowedRoles) {
        Set<String> roles = Arrays.stream((headerValue == null ? "" : headerValue).split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
        for (String allowedRole : allowedRoles) {
            if (roles.contains(allowedRole)) {
                return;
            }
        }
        throw new AdminAccessException("Insufficient admin permissions");
    }

    public static final class AdminAccessException extends RuntimeException {

        AdminAccessException(String message) {
            super(message);
        }

        HttpStatus status() {
            return HttpStatus.FORBIDDEN;
        }
    }
}
