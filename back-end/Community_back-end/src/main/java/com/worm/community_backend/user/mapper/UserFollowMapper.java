package com.worm.community_backend.user.mapper;

import com.worm.community_backend.user.vo.UserSimpleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface UserFollowMapper {

    @Insert("INSERT INTO user_follows (follower_id, following_id) VALUES (#{followerId}, #{followingId})")
    int insert(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    @Delete("DELETE FROM user_follows WHERE follower_id = #{followerId} AND following_id = #{followingId}")
    int delete(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    @Select("SELECT COUNT(*) FROM user_follows WHERE follower_id = #{followerId} AND following_id = #{followingId}")
    int countByFollowerAndFollowing(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    @Select("""
            SELECT u.id, u.username, u.nickname, u.avatar, u.bio
            FROM user_follows f JOIN users u ON u.id = f.following_id
            WHERE f.follower_id = #{userId}
            ORDER BY f.created_at DESC
            """)
    List<UserSimpleVO> selectFollowing(@Param("userId") Long userId);

    @Select("""
            SELECT u.id, u.username, u.nickname, u.avatar, u.bio
            FROM user_follows f JOIN users u ON u.id = f.follower_id
            WHERE f.following_id = #{userId}
            ORDER BY f.created_at DESC
            """)
    List<UserSimpleVO> selectFollowers(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM user_follows WHERE follower_id = #{userId}")
    long countFollowing(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM user_follows WHERE following_id = #{userId}")
    long countFollowers(@Param("userId") Long userId);

    @Select("""
            SELECT u.id, u.username, u.nickname, u.avatar, u.bio
            FROM user_follows f JOIN users u ON u.id = f.following_id
            WHERE f.follower_id = #{userId}
            ORDER BY f.created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<UserSimpleVO> selectFollowingPage(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);

    @Select("""
            SELECT u.id, u.username, u.nickname, u.avatar, u.bio
            FROM user_follows f JOIN users u ON u.id = f.follower_id
            WHERE f.following_id = #{userId}
            ORDER BY f.created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<UserSimpleVO> selectFollowersPage(@Param("userId") Long userId, @Param("offset") int offset, @Param("limit") int limit);
}
