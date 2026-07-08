package com.worm.community_backend.user.mapper;

import com.worm.community_backend.user.entity.RefreshToken;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 刷新令牌数据访问层。
 */
@Mapper
public interface RefreshTokenMapper {

    @Insert("""
            INSERT INTO refresh_tokens (user_id, token, expires_at, created_at, revoked)
            VALUES (#{userId}, #{token}, #{expiresAt}, #{createdAt}, #{revoked})
            """)
    int insert(RefreshToken refreshToken);

    @Select("""
            SELECT id, user_id, token, expires_at, created_at, revoked
            FROM refresh_tokens
            WHERE token = #{token} AND revoked = false
            """)
    RefreshToken selectByToken(String token);

    @Select("""
            SELECT id, user_id, token, expires_at, created_at, revoked
            FROM refresh_tokens
            WHERE user_id = #{userId} AND revoked = false AND expires_at > #{now}
            """)
    List<RefreshToken> selectActiveTokensByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @Update("""
            UPDATE refresh_tokens
            SET revoked = true
            WHERE token = #{token}
            """)
    int revokeToken(String token);

    @Update("""
            UPDATE refresh_tokens
            SET revoked = true
            WHERE user_id = #{userId} AND revoked = false
            """)
    int revokeAllTokensByUserId(Long userId);

    @Delete("""
            DELETE FROM refresh_tokens
            WHERE expires_at < #{now} OR revoked = true
            """)
    int deleteExpiredAndRevokedTokens(LocalDateTime now);

    @Select("""
            SELECT COUNT(*) FROM refresh_tokens
            WHERE user_id = #{userId} AND revoked = false AND expires_at > #{now}
            """)
    int countActiveTokensByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
