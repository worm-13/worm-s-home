package com.worm.community_backend.post.service;

import com.worm.community_backend.common.PageResponse;
import com.worm.community_backend.post.dto.PostCreateDTO;
import com.worm.community_backend.post.dto.PostUpdateDTO;
import com.worm.community_backend.post.vo.PostDetailVO;
import com.worm.community_backend.post.vo.PostListItemVO;

import java.util.List;

public interface PostService {
    Long createPost(Long userId, PostCreateDTO createDTO);

    void updatePost(Long userId, Long postId, PostUpdateDTO dto);

    void deletePost(Long userId, Long postId);

    List<PostListItemVO> listPostsOrderByDate();

    PostDetailVO getPostDetail(Long id, Long currentUserId);

    /** 保存草稿 */
    Long saveDraft(Long userId, PostCreateDTO createDTO);

    /** 发布草稿/定时文章为已发布 */
    void publishPost(Long userId, Long postId);

    /** 查看当前用户所有文章（含草稿、定时） */
    List<PostListItemVO> listMyPosts(Long userId);

    /** 查看当前用户草稿列表 */
    List<PostListItemVO> listMyDrafts(Long userId);

    /** 查看自己的文章详情（草稿/定时也可查看） */
    PostDetailVO getMyPostDetail(Long userId, Long postId);

    /** 定时任务：自动发布到期的定时文章 */
    void publishScheduledPosts();

    /** 分页：首页已发布文章列表 */
    PageResponse<PostListItemVO> listPostsPage(int page, int size);

    /** 分页：当前用户所有文章 */
    PageResponse<PostListItemVO> listMyPostsPage(Long userId, int page, int size);

    /** 分页：当前用户草稿列表 */
    PageResponse<PostListItemVO> listMyDraftsPage(Long userId, int page, int size);

    /** 分页：指定用户的已发布文章（公开查看） */
    PageResponse<PostListItemVO> listUserPostsPage(Long userId, int page, int size);
}
