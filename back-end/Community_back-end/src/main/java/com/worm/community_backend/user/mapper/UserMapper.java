package com.worm.community_backend.user.mapper;

import com.worm.community_backend.user.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

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

    @Insert("""
            INSERT INTO users (username, password, nickname, email)
            VALUES (#{username}, #{password}, #{nickname}, #{email})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
}
