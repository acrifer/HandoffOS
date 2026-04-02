package com.lifeos.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeos.common.response.Result;
import com.lifeos.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Slf4j
@Component
/**
 * 网关统一鉴权入口。
 *
 * 这里负责处理两类请求：
 * 1. 普通用户接口：/api/**
 * 2. 管理后台接口：/admin-api/**
 *
 * 处理顺序固定为：
 * 1. 先判断是否命中白名单
 * 2. 再解析 Authorization 中的 JWT
 * 3. 再去 Redis 校验该 JWT 是否仍然是“当前有效会话”
 * 4. 最后把身份信息透传给下游服务
 *
 * 这样做的原因是：
 * - JWT 负责无状态身份声明
 * - Redis 负责会话失效、踢下线、单点登录控制
 * 两者结合后，既保留了 JWT 的轻量性，也能主动让旧 token 失效。
 */
public class AuthFilter implements GlobalFilter, Ordered {

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    // ==================== Redis 作用：网关会话校验 ====================
    // 这里只做一件事：读取“当前最新有效 token”。
    // Redis 在网关里不是通用缓存，而是会话状态中心：
    // 1. 用户重新登录后，Redis 中只保留最新 token
    // 2. 用户退出登录后，对应 key 会被删除
    // 3. 管理员和普通用户使用不同前缀，彼此完全隔离
    // ==============================================================

    // 白名单路径，不需要登录即可访问
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/user/login",
            "/api/user/register",
            "/admin-api/admin/auth/login",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/service-docs/**");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getURI().getPath();

        // 1. 白名单接口不经过登录校验，直接放行。
        for (String whitePath : WHITE_LIST) {
            if (antPathMatcher.match(whitePath, path)) {
                return chain.filter(exchange);
            }
        }

        // 2. 所有受保护接口都必须显式携带 Authorization 头。
        String token = request.getHeaders().getFirst("Authorization");
        if (token == null || token.isEmpty()) {
            return authFailed(response, "Missing Authorization header");
        }

        // 3. 先做 JWT 的签名和过期时间校验，快速过滤掉伪造或过期 token。
        String jwtToken = JwtUtil.extractToken(token);
        Claims claims = JwtUtil.parseToken(jwtToken);

        if (claims == null) {
            return authFailed(response, "Invalid or expired token");
        }

        boolean adminPath = path.startsWith("/admin-api/");
        if (adminPath) {
            // 管理端接口除了 token 合法，还要求 token 内部带有 ADMIN 作用域。
            String scope = claims.get("scope", String.class);
            if (!"ADMIN".equalsIgnoreCase(scope)) {
                return authFailed(response, "Admin token required");
            }
            Long adminUserId = Long.parseLong(claims.getSubject());

            // ===== Redis 校验：admin:token:* =====
            // 管理员 token 单独存放在 admin:token:* 命名空间，避免和普通用户会话混用。
            // Redis 在这里的作用不是“缓存管理员资料”，而是做“管理员当前登录态校验”：
            // 只有 JWT 和 Redis 中保存的最新值一致，才说明这个管理员会话仍然有效。
            String redisToken = stringRedisTemplate.opsForValue().get("admin:token:" + adminUserId);
            if (!jwtToken.equals(redisToken)) {
                return authFailed(response, "Admin token expired or logged out");
            }

            // 网关负责把管理员身份透传给下游服务，避免每个服务重复解析 JWT。
            ServerHttpRequest newRequest = request.mutate()
                    .header("X-Admin-Id", String.valueOf(adminUserId))
                    .header("X-Admin-Username", claims.get("username", String.class))
                    .header("X-Admin-Roles", normalizeRoles(claims.get("roleCodes", String.class)))
                    .build();
            return chain.filter(exchange.mutate().request(newRequest).build());
        }

        // ===== Redis 校验：token:* =====
        // 普通业务接口走独立的用户会话空间 token:*。
        // Redis 在这里承担“最新会话真相源”的职责：
        // 1. JWT 只证明 token 自身是合法签发的
        // 2. Redis 再补一层“它是不是当前仍然允许使用的那一份”
        // 这样用户改密码、重新登录、被管理员禁用后，旧 token 都能立即被拒绝。
        Long userId = Long.parseLong(claims.getSubject());
        String redisToken = stringRedisTemplate.opsForValue().get("token:" + userId);
        if (!jwtToken.equals(redisToken)) {
            return authFailed(response, "Token expired or logged out");
        }

        // 普通用户接口只需要透传用户 id 和用户名。
        ServerHttpRequest newRequest = request.mutate()
                .header("X-User-Id", String.valueOf(userId))
                .header("X-User-Name", claims.get("username", String.class))
                .build();

        return chain.filter(exchange.mutate().request(newRequest).build());
    }

    private Mono<Void> authFailed(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");

        Result<Object> result = Result.error(401, message);
        String resultString;
        try {
            resultString = objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            resultString = "{\"code\":401,\"message\":\"" + message + "\"}";
        }

        DataBuffer buffer = response.bufferFactory().wrap(resultString.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private String normalizeRoles(String roleCodes) {
        if (roleCodes == null || roleCodes.isBlank()) {
            return "";
        }
        return roleCodes.trim().toUpperCase(Locale.ROOT);
    }
}
