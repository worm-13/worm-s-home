package com.worm.community_backend.post.service.impl;

import com.worm.community_backend.common.PageResponse;
import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.interaction.service.InteractionService;
import com.worm.community_backend.moderation.service.SensitiveWordFilter;
import com.worm.community_backend.post.dto.PostCreateDTO;
import com.worm.community_backend.post.dto.PostUpdateDTO;
import com.worm.community_backend.post.entity.Post;
import com.worm.community_backend.post.mapper.PostMapper;
import com.worm.community_backend.post.service.PostService;
import com.worm.community_backend.post.vo.PostDetailVO;
import com.worm.community_backend.post.vo.PostListItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private static final int SUMMARY_MAX_LENGTH = 100;
    private static final int TITLE_MAX_LENGTH = 120;
    private static final int CONTENT_MAX_LENGTH = 50000;
    private final PostMapper postMapper;
    private final InteractionService interactionService;
    private final SensitiveWordFilter sensitiveWordFilter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPost(Long userId, PostCreateDTO createDTO) {
        validateUserId(userId);
        if (createDTO == null)
            throw new BusinessException(ResultCode.BAD_REQUEST);

        String title = requireNonBlank(createDTO.getTitle(), ResultCode.POST_TITLE_REQUIRED);
        String content = requireNonBlank(createDTO.getContent(), ResultCode.POST_CONTENT_REQUIRED);

        // 内容长度限制
        if (title.length() > TITLE_MAX_LENGTH) {
            throw new BusinessException(1047, "title exceeds maximum length of " + TITLE_MAX_LENGTH);
        }
        if (content.length() > CONTENT_MAX_LENGTH) {
            throw new BusinessException(1048, "content exceeds maximum length of " + CONTENT_MAX_LENGTH);
        }

        // 敏感词过滤
        if (sensitiveWordFilter.containsSensitiveWord(title) || sensitiveWordFilter.containsSensitiveWord(content)) {
            throw new BusinessException(1049, "content contains sensitive words");
        }

        int status = (createDTO.getStatus() != null) ? createDTO.getStatus() : 1;

        // 定时发布校验
        if (status == 3) {
            if (createDTO.getScheduledAt() == null)
                throw new BusinessException(ResultCode.POST_SCHEDULE_TIME_REQUIRED);
            if (createDTO.getScheduledAt().isBefore(LocalDateTime.now()))
                throw new BusinessException(ResultCode.POST_SCHEDULE_TIME_PAST);
        }

        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(title);
        post.setContent(content);
        post.setSummary(buildSummary(content, createDTO.getSummary()));
        post.setCoverImage(trimToNull(createDTO.getCoverImage()));
        post.setStatus(status);
        post.setScheduledAt(status == 3 ? createDTO.getScheduledAt() : null);

        if (postMapper.insertPost(post) != 1 || post.getId() == null) {
            throw new BusinessException(ResultCode.POST_CREATE_FAILED);
        }
        // 只有发布状态才增加用户文章计数
        if (status == 1) {
            if (postMapper.increaseUserPostsCount(userId) != 1) {
                throw new BusinessException(ResultCode.POST_CREATE_FAILED);
            }
        }
        return post.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "postDetails", key = "#postId")
    public void updatePost(Long userId, Long postId, PostUpdateDTO dto) {
        validateUserId(userId);
        if (postId == null || dto == null)
            throw new BusinessException(ResultCode.BAD_REQUEST);

        Post post = requirePostOwner(postId, userId);
        if (post.getStatus() == 0)
            throw new BusinessException(ResultCode.NOT_FOUND);

        String title = trimToNull(dto.getTitle());
        String content = trimToNull(dto.getContent());
        if (title != null && content != null) {
            postMapper.updatePost(postId, title, content,
                    buildSummary(content, dto.getSummary()), trimToNull(dto.getCoverImage()));
        }

        // 更新状态（草稿→发布、草稿→定时 等）
        if (dto.getStatus() != null) {
            int newStatus = dto.getStatus();
            if (newStatus == 3) {
                if (dto.getScheduledAt() == null)
                    throw new BusinessException(ResultCode.POST_SCHEDULE_TIME_REQUIRED);
                if (dto.getScheduledAt().isBefore(LocalDateTime.now()))
                    throw new BusinessException(ResultCode.POST_SCHEDULE_TIME_PAST);
            }
            if (newStatus == 1 && post.getStatus() != 1) {
                // 从草稿/定时发布为已发布，需要增加用户文章计数
                if (postMapper.increaseUserPostsCount(userId) != 1) {
                    throw new BusinessException(ResultCode.POST_PUBLISH_FAILED);
                }
            }
            postMapper.updatePostStatus(postId, newStatus,
                    newStatus == 3 ? dto.getScheduledAt() : null);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePost(Long userId, Long postId) {
        validateUserId(userId);
        if (postId == null)
            throw new BusinessException(ResultCode.BAD_REQUEST);
        requirePostOwner(postId, userId);
        if (postMapper.softDelete(postId) != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
    }

    @Override
    public List<PostListItemVO> listPostsOrderByDate() {
        return postMapper.selectPublishedPostsOrderByCreatedAtDesc();
    }

    @Override
    @Cacheable(value = "postDetails", key = "#id")
    public PostDetailVO getPostDetail(Long id, Long currentUserId) {
        if (id == null || id <= 0)
            throw new BusinessException(ResultCode.BAD_REQUEST);
        PostDetailVO detail = postMapper.selectPublishedPostDetailById(id);
        if (detail == null)
            throw new BusinessException(ResultCode.NOT_FOUND);

        if (detail.getLikesCount() == null)
            detail.setLikesCount(0);
        if (detail.getFavoritesCount() == null)
            detail.setFavoritesCount(0);

        if (currentUserId != null) {
            Map<String, Boolean> status = interactionService.getInteractionStatus(currentUserId, id);
            detail.setIsLiked(status.getOrDefault("isLiked", false));
            detail.setIsFavorited(status.getOrDefault("isFavorited", false));
        } else {
            detail.setIsLiked(false);
            detail.setIsFavorited(false);
        }

        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveDraft(Long userId, PostCreateDTO createDTO) {
        if (createDTO == null)
            throw new BusinessException(ResultCode.BAD_REQUEST);
        // 强制为草稿状态
        createDTO.setStatus(2);
        createDTO.setScheduledAt(null);
        return createPost(userId, createDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishPost(Long userId, Long postId) {
        validateUserId(userId);
        if (postId == null)
            throw new BusinessException(ResultCode.BAD_REQUEST);

        Post post = requirePostOwner(postId, userId);
        if (post.getStatus() == 1)
            return; // 已经是发布状态
        if (post.getStatus() == 0)
            throw new BusinessException(ResultCode.NOT_FOUND);

        if (postMapper.publishPost(postId) != 1) {
            throw new BusinessException(ResultCode.POST_PUBLISH_FAILED);
        }
        // 增加用户文章计数
        if (postMapper.increaseUserPostsCount(userId) != 1) {
            throw new BusinessException(ResultCode.POST_PUBLISH_FAILED);
        }
    }

    @Override
    public List<PostListItemVO> listMyPosts(Long userId) {
        validateUserId(userId);
        return postMapper.selectMyPosts(userId);
    }

    @Override
    public List<PostListItemVO> listMyDrafts(Long userId) {
        validateUserId(userId);
        return postMapper.selectMyDrafts(userId);
    }

    @Override
    public PostDetailVO getMyPostDetail(Long userId, Long postId) {
        validateUserId(userId);
        if (postId == null || postId <= 0)
            throw new BusinessException(ResultCode.BAD_REQUEST);
        PostDetailVO detail = postMapper.selectMyPostDetailById(postId, userId);
        if (detail == null)
            throw new BusinessException(ResultCode.NOT_FOUND);

        if (detail.getLikesCount() == null)
            detail.setLikesCount(0);
        if (detail.getFavoritesCount() == null)
            detail.setFavoritesCount(0);

        Map<String, Boolean> interactionStatus = interactionService.getInteractionStatus(userId, postId);
        detail.setIsLiked(interactionStatus.getOrDefault("isLiked", false));
        detail.setIsFavorited(interactionStatus.getOrDefault("isFavorited", false));

        return detail;
    }

    @Override
    public PageResponse<PostListItemVO> listPostsPage(int page, int size) {
        int offset = (page - 1) * size;
        long total = postMapper.countPublishedPosts();
        List<PostListItemVO> items = postMapper.selectPublishedPostsPage(offset, size);
        boolean hasMore = offset + size < total;
        return new PageResponse<>(items, total, page, size, hasMore);
    }

    @Override
    public PageResponse<PostListItemVO> listMyPostsPage(Long userId, int page, int size) {
        validateUserId(userId);
        int offset = (page - 1) * size;
        long total = postMapper.countMyPosts(userId);
        List<PostListItemVO> items = postMapper.selectMyPostsPage(userId, offset, size);
        boolean hasMore = offset + size < total;
        return new PageResponse<>(items, total, page, size, hasMore);
    }

    @Override
    public PageResponse<PostListItemVO> listMyDraftsPage(Long userId, int page, int size) {
        validateUserId(userId);
        int offset = (page - 1) * size;
        long total = postMapper.countMyDrafts(userId);
        List<PostListItemVO> items = postMapper.selectMyDraftsPage(userId, offset, size);
        boolean hasMore = offset + size < total;
        return new PageResponse<>(items, total, page, size, hasMore);
    }

    @Override
    public PageResponse<PostListItemVO> listUserPostsPage(Long userId, int page, int size) {
        validateUserId(userId);
        int offset = (page - 1) * size;
        long total = postMapper.countUserPublishedPosts(userId);
        List<PostListItemVO> items = postMapper.selectUserPublishedPostsPage(userId, offset, size);
        boolean hasMore = offset + size < total;
        return new PageResponse<>(items, total, page, size, hasMore);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishScheduledPosts() {
        List<Post> scheduledPosts = postMapper.selectScheduledPostsToPublish();
        for (Post post : scheduledPosts) {
            try {
                postMapper.publishPost(post.getId());
                postMapper.increaseUserPostsCount(post.getUserId());
                log.info("定时发布文章成功, postId={}", post.getId());
            } catch (Exception e) {
                log.error("定时发布文章失败, postId={}", post.getId(), e);
            }
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0)
            throw new BusinessException(ResultCode.BAD_REQUEST);
    }

    private Post requirePostOwner(Long postId, Long userId) {
        Post post = postMapper.selectById(postId);
        if (post == null || !post.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return post;
    }

    private String requireNonBlank(String value, ResultCode code) {
        String trimmed = trimToNull(value);
        if (trimmed == null)
            throw new BusinessException(code);
        return trimmed;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String buildSummary(String content, String summary) {
        String s = trimToNull(summary);
        if (s != null)
            return s;
        return content.length() <= SUMMARY_MAX_LENGTH ? content : content.substring(0, SUMMARY_MAX_LENGTH);
    }
}
