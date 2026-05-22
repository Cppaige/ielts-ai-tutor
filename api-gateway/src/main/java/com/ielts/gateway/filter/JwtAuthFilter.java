package com.ielts.gateway.filter;

import com.ielts.gateway.util.JwtValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
@Order(1)
public class JwtAuthFilter extends OncePerRequestFilter {

    // 1. 修改点：加上 /api 前缀，兼容网关的真实路由
    private static final Set<String> WHITELIST = Set.of(
            "/auth/login", "/auth/register",
            "/api/auth/login", "/api/auth/register"
    );

    private final JwtValidator jwtValidator;

    public JwtAuthFilter(JwtValidator jwtValidator) {
        this.jwtValidator = jwtValidator;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 2. 核心大招：无条件放行所有的 OPTIONS 预检请求！
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (WHITELIST.stream().anyMatch(path::startsWith)) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        //logger.info("拿到的Header: " + authHeader);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(response);
            return;
        }

        String token = authHeader.substring(7);
        Optional<Long> userId = jwtValidator.validate(token);
        if (userId.isEmpty()) {
            sendUnauthorized(response);
            return;
        }

        HttpServletRequest wrappedRequest = new UserIdHeaderWrapper(request, userId.get());
        chain.doFilter(wrappedRequest, response);
    }

    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":401,\"message\":\"Unauthorized\"}");
    }

    // ... UserIdHeaderWrapper 内部类保持不变 ...
    private static class UserIdHeaderWrapper extends HttpServletRequestWrapper {
        private final Long userId;

        UserIdHeaderWrapper(HttpServletRequest request, Long userId) {
            super(request);
            this.userId = userId;
        }

        @Override
        public String getHeader(String name) {
            if ("X-User-Id".equalsIgnoreCase(name)) {
                return String.valueOf(userId);
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = Collections.list(super.getHeaderNames());
            names.add("X-User-Id");
            return Collections.enumeration(names);
        }
    }
}