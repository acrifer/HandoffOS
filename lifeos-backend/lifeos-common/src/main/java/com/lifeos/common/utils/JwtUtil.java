package com.lifeos.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class JwtUtil {

    private static final String SECRET_KEY_STR = getRequiredSetting("LIFEOS_JWT_SECRET");
    private static final long EXPIRATION_TIME = getLongSetting("LIFEOS_JWT_EXPIRATION_MS", 86400000L);

    private static String getRequiredSetting(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            value = System.getenv(name);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must be configured.");
        }
        if (value.length() < 32) {
            throw new IllegalStateException(name + " must be at least 32 characters long.");
        }
        return value;
    }

    private static long getLongSetting(String name, long defaultValue) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            value = System.getenv(name);
        }
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Long.parseLong(value);
    }

    private static Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY_STR.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate token
     */
    public static String generateToken(Long userId, String username) {
        return generateToken(userId, username, Map.of());
    }

    public static String generateToken(Long userId, String username, Map<String, Object> claims) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_TIME);

        var builder = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setId(UUID.randomUUID().toString())
                .claim("username", username)
                .setIssuedAt(now)
                .setExpiration(expiration);

        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            builder.claim(entry.getKey(), entry.getValue());
        }

        return builder.signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();
    }

    /**
     * Parse token
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null; // Invalid token
        }
    }

    /**
     * Get userId from token
     */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims != null && claims.getSubject() != null) {
            return Long.parseLong(claims.getSubject());
        }
        return null;
    }

    public static long getExpirationTimeMs() {
        return EXPIRATION_TIME;
    }
    
    /**
     * Validate Auth header format
     */
    public static String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader;
    }
}
