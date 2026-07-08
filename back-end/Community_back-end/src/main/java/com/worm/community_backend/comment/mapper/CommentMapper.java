package com.worm.community_backend.comment.mapper;

import com.worm.community_backend.comment.entity.Comment;
import com.worm.community_backend.comment.vo.CommentVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommentMapper {

        @Insert("INSERT INTO comments (post_id, user_id, parent_id, content) VALUES (#{postId}, #{userId}, #{parentId}, #{content})")
        @Options(useGeneratedKeys = true, keyProperty = "id")
        int insert(Comment comment);

        @Update("UPDATE posts SET comment_count = comment_count + 1 WHERE id = #{postId}")
        int increasePostCommentCount(@Param("postId") Long postId);

        @Select("""
                        SELECT c.id, c.post_id AS postId, c.user_id AS userId, u.username, u.nickname, u.avatar,
                               c.parent_id AS parentId, c.content, c.created_at AS createdAt
                        FROM comments c JOIN users u ON u.id = c.user_id
                        WHERE c.post_id = #{postId} AND c.status = 1
                        ORDER BY c.created_at ASC
                        """)
        List<CommentVO> selectByPostId(@Param("postId") Long postId);

        @Select("SELECT id, user_id, status FROM comments WHERE id = #{id}")
        Comment selectById(@Param("id") Long id);

        @Update("UPDATE comments SET status = 0 WHERE id = #{id} AND status = 1")
        int softDelete(@Param("id") Long id);

        @Update("UPDATE posts SET comment_count = comment_count - 1 WHERE id = #{postId} AND comment_count > 0")
        int decreasePostCommentCount(@Param("postId") Long postId);

        @Select("""
                        SELECT c.id, c.post_id AS postId, c.user_id AS userId, u.username, u.nickname, u.avatar,
                               c.parent_id AS parentId, c.content, c.created_at AS createdAt
                        FROM comments c JOIN users u ON u.id = c.user_id
                        WHERE c.post_id = #{postId} AND c.status = 1
                        ORDER BY c.created_at ASC
                        LIMIT #{offset}, #{size}
                        """)
        List<CommentVO> selectByPostIdPage(@Param("postId") Long postId, @Param("offset") int offset,
                        @Param("size") int size);

        @Select("SELECT COUNT(*) FROM comments WHERE post_id = #{postId} AND status = 1")
        long countByPostId(@Param("postId") Long postId);
}
