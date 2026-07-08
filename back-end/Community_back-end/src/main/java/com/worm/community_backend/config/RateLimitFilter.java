package com.worm.community_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worm.community_backend.common.ApiResponse;
import com.worm.community_backend.common.ResultCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 速率限制过滤器：对登录和注册接口进行速率限制。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = getClientIp(request);

        // 只对POST请求的登录和注册接口进行速率限制
        if ("POST".equals(method)) {
            if ("/api/auth/login".equals(requestUri)) {
                if (!rateLimiter.isLoginAllowed(clientIp)) {
                    log.warn("Login rate limit exceeded for IP: {}", clientIp);
                    sendRateLimitResponse(response);
                    return;
                }
                rateLimiter.recordLoginAttempt(clientIp);
            } else if ("/api/auth/register".equals(requestUri)) {
                if (!rateLimiter.isRegisterAllowed(clientIp)) {
                    log.warn("Register rate limit exceeded for IP: {}", clientIp);
                    sendRateLimitResponse(response);
                    return;
                }
                rateLimiter.recordRegisterAttempt(clientIp);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // 取第一个IP（客户端真实IP）
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private void sendRateLimitResponse(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");

        ApiResponse<Object> apiResponse = ApiResponse.error(ResultCode.RATE_LIMIT_EXCEEDED);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
