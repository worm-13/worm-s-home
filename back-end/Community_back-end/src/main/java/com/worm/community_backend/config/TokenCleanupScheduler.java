package com.worm.community_backend.config;

import com.worm.community_backend.user.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：清理过期的刷新令牌。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final RefreshTokenService refreshTokenService;

    /**
     * 每天凌晨2点清理过期的刷新令牌。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired refresh tokens");
        refreshTokenService.cleanupExpiredTokens();
        log.info("Completed cleanup of expired refresh tokens");
    }
}
