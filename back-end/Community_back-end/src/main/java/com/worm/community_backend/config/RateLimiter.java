package com.worm.community_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 速率限制器：用于限制登录和注册接口的请求频率。
 */
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
@Data
public class RateLimiter {

    private boolean enabled = true;
    private int loginMaxAttempts = 5;
    private int loginWindowSeconds = 300;
    private int registerMaxAttempts = 3;
    private int registerWindowSeconds = 3600;

    private final ConcurrentHashMap<String, AttemptInfo> loginAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AttemptInfo> registerAttempts = new ConcurrentHashMap<>();

    /**
     * 检查登录请求是否被允许。
     */
    public boolean isLoginAllowed(String clientIp) {
        if (!enabled) {
            return true;
        }
        return isAllowed(clientIp, loginAttempts, loginMaxAttempts, loginWindowSeconds);
    }

    /**
     * 检查注册请求是否被允许。
     */
    public boolean isRegisterAllowed(String clientIp) {
        if (!enabled) {
            return true;
        }
        return isAllowed(clientIp, registerAttempts, registerMaxAttempts, registerWindowSeconds);
    }

    /**
     * 记录登录尝试。
     */
    public void recordLoginAttempt(String clientIp) {
        if (!enabled) {
            return;
        }
        recordAttempt(clientIp, loginAttempts, loginWindowSeconds);
    }

    /**
     * 记录注册尝试。
     */
    public void recordRegisterAttempt(String clientIp) {
        if (!enabled) {
            return;
        }
        recordAttempt(clientIp, registerAttempts, registerWindowSeconds);
    }

    /**
     * 重置登录尝试计数（登录成功后调用）。
     */
    public void resetLoginAttempts(String clientIp) {
        loginAttempts.remove(clientIp);
    }

    /**
     * 重置注册尝试计数（注册成功后调用）。
     */
    public void resetRegisterAttempts(String clientIp) {
        registerAttempts.remove(clientIp);
    }

    private boolean isAllowed(String clientIp, ConcurrentHashMap<String, AttemptInfo> attempts, int maxAttempts, int windowSeconds) {
        AttemptInfo attemptInfo = attempts.get(clientIp);
        if (attemptInfo == null) {
            return true;
        }

        // 检查是否在时间窗口内
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - (windowSeconds * 1000L);

        if (attemptInfo.getFirstAttemptTime() < windowStart) {
            // 时间窗口已过期，重置计数
            attempts.remove(clientIp);
            return true;
        }

        return attemptInfo.getCount() < maxAttempts;
    }

    private void recordAttempt(String clientIp, ConcurrentHashMap<String, AttemptInfo> attempts, int windowSeconds) {
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - (windowSeconds * 1000L);

        attempts.compute(clientIp, (key, existing) -> {
            if (existing == null || existing.getFirstAttemptTime() < windowStart) {
                // 新的尝试或时间窗口已过期
                return new AttemptInfo(1, currentTime);
            } else {
                // 在时间窗口内增加计数
                return new AttemptInfo(existing.getCount() + 1, existing.getFirstAttemptTime());
            }
        });
    }

    /**
     * 尝试信息内部类。
     */
    @Data
    private static class AttemptInfo {
        private final int count;
        private final long firstAttemptTime;
    }
}
