package com.worm.community_backend.user.service;

import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.config.JwtTokenProvider;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.user.entity.RefreshToken;
import com.worm.community_backend.user.mapper.RefreshTokenMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 刷新令牌服务：管理刷新令牌的生成、验证和撤销。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenMapper refreshTokenMapper;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 为用户生成刷新令牌。
     */
    @Transactional
    public String createRefreshToken(Long userId) {
        // 撤销该用户的所有现有刷新令牌（单设备登录策略）
        refreshTokenMapper.revokeAllTokensByUserId(userId);

        String token = jwtTokenProvider.generateRefreshToken();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusSeconds(jwtTokenProvider.getRefreshExpirationSeconds());

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(token)
                .expiresAt(expiresAt)
                .createdAt(now)
                .revoked(false)
                .build();

        refreshTokenMapper.insert(refreshToken);
        log.info("Created refresh token for user: {}", userId);

        return token;
    }

    /**
     * 验证刷新令牌并返回用户ID。
     */
    @Transactional
    public Long validateRefreshToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_REQUIRED);
        }

        RefreshToken refreshToken = refreshTokenMapper.selectByToken(token);
        if (refreshToken == null) {
            throw new BusinessException(ResultCode.REFRESH_TOKEN_INVALID);
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            // 令牌已过期，撤销它
            refreshTokenMapper.revokeToken(token);
            throw new BusinessException(ResultCode.REFRESH_TOKEN_EXPIRED);
        }

        return refreshToken.getUserId();
    }

    /**
     * 撤销刷新令牌。
     */
    @Transactional
    public void revokeRefreshToken(String token) {
        if (token != null && !token.isEmpty()) {
            refreshTokenMapper.revokeToken(token);
            log.info("Revoked refresh token");
        }
    }

    /**
     * 撤销用户的所有刷新令牌。
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenMapper.revokeAllTokensByUserId(userId);
        log.info("Revoked all refresh tokens for user: {}", userId);
    }

    /**
     * 清理过期和已撤销的令牌。
     */
    @Transactional
    public void cleanupExpiredTokens() {
        int deleted = refreshTokenMapper.deleteExpiredAndRevokedTokens(LocalDateTime.now());
        log.info("Cleaned up {} expired/revoked refresh tokens", deleted);
    }
}
