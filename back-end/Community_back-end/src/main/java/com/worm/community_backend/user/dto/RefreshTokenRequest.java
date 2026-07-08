package com.worm.community_backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新令牌请求 DTO。
 */
@Data
public class RefreshTokenRequest {
    @NotBlank(message = "refresh token is required")
    private String refreshToken;
}
