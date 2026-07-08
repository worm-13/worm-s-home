package com.worm.community_backend.post.mapper;

import com.worm.community_backend.post.entity.Post;
import com.worm.community_backend.post.vo.PostDetailVO;
import com.worm.community_backend.post.vo.PostListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PostMapper {

        int insertPost(Post post);

        int increaseUserPostsCount(@Param("userId") Long userId);

        List<PostListItemVO> selectPublishedPostsOrderByCreatedAtDesc();

        PostDetailVO selectPublishedPostDetailById(@Param("id") Long id);

        @Select("SELECT id, user_id, status FROM posts WHERE id = #{id}")
        Post selectById(@Param("id") Long id);

        int updatePost(@Param("id") Long id, @Param("title") String title,
                        @Param("content") String content, @Param("summary") String summary,
                        @Param("coverImage") String coverImage);

        @Update("UPDATE posts SET status = 0, updated_at = NOW() WHERE id = #{id} AND status IN (1, 2, 3)")
        int softDelete(@Param("id") Long id);

        int updatePostStatus(@Param("id") Long id, @Param("status") Integer status,
                        @Param("scheduledAt") java.time.LocalDateTime scheduledAt);

        List<PostListItemVO> selectMyPosts(@Param("userId") Long userId);

        List<PostListItemVO> selectMyDrafts(@Param("userId") Long userId);

        List<Post> selectScheduledPostsToPublish();

        // ---- 分页查询 ----
        List<PostListItemVO> selectPublishedPostsPage(@Param("offset") int offset, @Param("size") int size);

        long countPublishedPosts();

        List<PostListItemVO> selectMyPostsPage(@Param("userId") Long userId, @Param("offset") int offset,
                        @Param("size") int size);

        long countMyPosts(@Param("userId") Long userId);

        List<PostListItemVO> selectMyDraftsPage(@Param("userId") Long userId, @Param("offset") int offset,
                        @Param("size") int size);

        long countMyDrafts(@Param("userId") Long userId);

        List<PostListItemVO> selectUserPublishedPostsPage(@Param("userId") Long userId, @Param("offset") int offset,
                        @Param("size") int size);

        long countUserPublishedPosts(@Param("userId") Long userId);

        int publishPost(@Param("id") Long id);

        PostDetailVO selectMyPostDetailById(@Param("id") Long id, @Param("userId") Long userId);
}
