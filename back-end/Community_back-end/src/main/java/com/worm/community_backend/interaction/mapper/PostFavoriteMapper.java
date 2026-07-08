package com.worm.community_backend.interaction.mapper;

import com.worm.community_backend.post.vo.PostListItemVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PostFavoriteMapper {

    @Insert("INSERT INTO post_favorites (post_id, user_id) VALUES (#{postId}, #{userId})")
    int insert(@Param("postId") Long postId, @Param("userId") Long userId);

    @Delete("DELETE FROM post_favorites WHERE post_id = #{postId} AND user_id = #{userId}")
    int delete(@Param("postId") Long postId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM post_favorites WHERE post_id = #{postId} AND user_id = #{userId}")
    int count(@Param("postId") Long postId, @Param("userId") Long userId);

    @Update("UPDATE posts SET favorite_count = favorite_count + 1 WHERE id = #{postId}")
    int increasePostFavoriteCount(@Param("postId") Long postId);

    @Update("UPDATE posts SET favorite_count = favorite_count - 1 WHERE id = #{postId} AND favorite_count > 0")
    int decreasePostFavoriteCount(@Param("postId") Long postId);

    @Select("""
            SELECT p.id, p.user_id AS userId, u.username AS authorName, u.avatar AS authorAvatar,
                   p.title, p.summary, p.cover_image AS coverImage, p.created_at AS createdAt
            FROM post_favorites f JOIN posts p ON p.id = f.post_id JOIN users u ON u.id = p.user_id
            WHERE f.user_id = #{userId} AND p.status = 1
            ORDER BY f.created_at DESC
            """)
    List<PostListItemVO> selectByUserId(@Param("userId") Long userId);
}
