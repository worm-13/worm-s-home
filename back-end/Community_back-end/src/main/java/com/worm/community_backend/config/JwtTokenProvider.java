package com.worm.community_backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具类：负责生成、校验并解析令牌。
 */
@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank() || secret.contains("replace-this")) {
            throw new IllegalStateException(
                    "JWT_SECRET 环境变量未设置或使用了占位符。请设置一个至少32字节的随机密钥。");
        }
        this.signingKey = createSigningKey(secret);
    }

    /** 根据用户 ID 和角色生成访问令牌。 */
    public String generateAccessToken(Long userId, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(jwtProperties.getExpirationSeconds());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /** 根据用户 ID 生成刷新令牌。 */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    /** 校验令牌签名与有效期。 */
    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /** 从令牌中提取用户 ID。 */
    public Long getUserId(String token) {
        Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
        return Long.parseLong(claims.getSubject());
    }

    /** 从令牌中提取用户角色。 */
    public String getRole(String token) {
        Claims claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
        return claims.get("role", String.class);
    }

    public long getExpirationSeconds() {
        return jwtProperties.getExpirationSeconds();
    }

    public long getRefreshExpirationSeconds() {
        return jwtProperties.getRefreshExpirationSeconds();
    }

    private static SecretKey createSigningKey(String secret) {
        // 优先按 Base64 解码使用，解码失败则按原始字符串处理。
        byte[] rawBytes = secret.getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = rawBytes;
        try {
            byte[] decoded = Decoders.BASE64.decode(secret);
            if (decoded.length >= 32) {
                keyBytes = decoded;
            }
        } catch (RuntimeException ex) {
            keyBytes = rawBytes;
        }

        // HMAC-SHA 算法要求最小密钥长度，长度不足时进行补齐。
        if (keyBytes.length < 32) {
            keyBytes = Arrays.copyOf(rawBytes, 32);
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }
}


