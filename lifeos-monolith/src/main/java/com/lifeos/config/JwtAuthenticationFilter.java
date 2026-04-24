package com.lifeos.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip authentication for public endpoints
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);

        if (token != null && jwtTokenUtil.validateToken(token)) {
            Long userId = jwtTokenUtil.getUserIdFromToken(token);
            request.setAttribute("userId", userId);
            request.setAttribute("username", jwtTokenUtil.getUsernameFromToken(token));
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Unauthorized\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String path) {
        return path.contains("/auth/login")
            || path.contains("/auth/register")
            || path.contains("/swagger-ui")
            || path.contains("/v3/api-docs")
            || path.contains("/actuator");
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
