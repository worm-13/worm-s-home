package com.worm.community_backend.user.mapper;

import com.worm.community_backend.user.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户数据访问层。
 */
@Mapper
public interface UserMapper {

    @Select("""
            SELECT id, username, password, nickname, email, avatar, background_image, bio,
                   gender, birthday, location, followers_count, following_count, posts_count,
                   status, role, last_login_at, created_at
            FROM users
            WHERE id = #{id}
            """)
    User selectById(Long id);

    @Select("""
            SELECT id, username, password, nickname, email, avatar, background_image, bio,
                   gender, birthday, location, followers_count, following_count, posts_count,
                   status, role, last_login_at, created_at
            FROM users
            WHERE username = #{username}
            """)
    User selectByUsername(String username);

    @Select("""
            SELECT id, username, password, nickname, email, avatar, background_image, bio,
                   gender, birthday, location, followers_count, following_count, posts_count,
                   status, role, last_login_at, created_at
            FROM users
            WHERE email = #{email}
            """)
    User selectByEmail(String email);

    /** 新增用户基础信息。 */
    @Insert("""
            INSERT INTO users (id, username, password, nickname, email)
            VALUES (#{id}, #{username}, #{password}, #{nickname}, #{email})
            """)
    int insert(User user);

    /** 更新最后登录时间。 */
    @Update("""
            UPDATE users
            SET last_login_at = #{lastLoginAt}
            WHERE id = #{id}
            """)
    int updateLastLoginAt(@Param("id") Long id, @Param("lastLoginAt") java.time.LocalDateTime lastLoginAt);

    /** 更新头像地址。 */
    @Update("""
            UPDATE users
            SET avatar = #{avatar}
            WHERE id = #{id}
            """)
    int updateAvatarById(@Param("id") Long id, @Param("avatar") String avatar);

    /** 更新背景图地址。 */
    @Update("""
            UPDATE users
            SET background_image = #{backgroundImage}
            WHERE id = #{id}
            """)
    int updateBackgroundImageById(@Param("id") Long id, @Param("backgroundImage") String backgroundImage);

    /** 更新用户基本资料。 */
    @Update("""
            UPDATE users
            SET nickname = #{nickname},
                bio = #{bio},
                gender = #{gender},
                birthday = #{birthday},
                location = #{location}
            WHERE id = #{id}
            """)
    int updateProfileById(@Param("id") Long id,
                          @Param("nickname") String nickname,
                          @Param("bio") String bio,
                          @Param("gender") Integer gender,
                          @Param("birthday") java.time.LocalDate birthday,
                          @Param("location") String location);

    @Update("UPDATE users SET followers_count = followers_count + #{count} WHERE id = #{id}")
    int updateFollowersCount(@Param("id") Long id, @Param("count") int count);

    @Update("UPDATE users SET following_count = following_count + #{count} WHERE id = #{id}")
    int updateFollowingCount(@Param("id") Long id, @Param("count") int count);

    @Update("UPDATE users SET password = #{password} WHERE id = #{id}")
    int updatePasswordById(@Param("id") Long id, @Param("password") String password);
}
