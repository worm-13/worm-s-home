package com.worm.community_backend.interaction.mapper;

import org.apache.ibatis.annotations.*;

@Mapper
public interface PostLikeMapper {

    @Insert("INSERT INTO post_likes (post_id, user_id) VALUES (#{postId}, #{userId})")
    int insert(@Param("postId") Long postId, @Param("userId") Long userId);

    @Delete("DELETE FROM post_likes WHERE post_id = #{postId} AND user_id = #{userId}")
    int delete(@Param("postId") Long postId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM post_likes WHERE post_id = #{postId} AND user_id = #{userId}")
    int count(@Param("postId") Long postId, @Param("userId") Long userId);

    @Update("UPDATE posts SET like_count = like_count + 1 WHERE id = #{postId}")
    int increasePostLikeCount(@Param("postId") Long postId);

    @Update("UPDATE posts SET like_count = like_count - 1 WHERE id = #{postId} AND like_count > 0")
    int decreasePostLikeCount(@Param("postId") Long postId);
}
