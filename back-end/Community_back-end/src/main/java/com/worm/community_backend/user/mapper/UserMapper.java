package com.worm.community_backend.user.mapper;

import com.worm.community_backend.user.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {

    @Select("""
            SELECT id, username, password, nickname, email, avatar, background_image, bio,
                   gender, birthday, location, followers_count, following_count, posts_count,
                   status, last_login_at, created_at
            FROM users
            WHERE id = #{id}
            """)
    User selectById(Long id);

    @Select("""
            SELECT id, username, password, nickname, email, avatar, background_image, bio,
                   gender, birthday, location, followers_count, following_count, posts_count,
                   status, last_login_at, created_at
            FROM users
            WHERE username = #{username}
            """)
    User selectByUsername(String username);

    @Select("""
            SELECT id, username, password, nickname, email, avatar, background_image, bio,
                   gender, birthday, location, followers_count, following_count, posts_count,
                   status, last_login_at, created_at
            FROM users
            WHERE email = #{email}
            """)
    User selectByEmail(String email);

    @Insert("""
            INSERT INTO users (id, username, password, nickname, email)
            VALUES (#{id}, #{username}, #{password}, #{nickname}, #{email})
            """)
    int insert(User user);

    @Update("""
            UPDATE users
            SET last_login_at = #{lastLoginAt}
            WHERE id = #{id}
            """)
    int updateLastLoginAt(@Param("id") Long id, @Param("lastLoginAt") java.time.LocalDateTime lastLoginAt);
}
