package com.worm.community_backend.interaction.service.impl;

import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.interaction.mapper.PostFavoriteMapper;
import com.worm.community_backend.interaction.mapper.PostLikeMapper;
import com.worm.community_backend.interaction.service.InteractionService;
import com.worm.community_backend.notification.service.NotificationService;
import com.worm.community_backend.post.entity.Post;
import com.worm.community_backend.post.mapper.PostMapper;
import com.worm.community_backend.post.vo.PostListItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InteractionServiceImpl implements InteractionService {

    private final PostLikeMapper postLikeMapper;
    private final PostFavoriteMapper postFavoriteMapper;
    private final PostMapper postMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void like(Long userId, Long postId) {
        validate(userId, postId);
        if (postLikeMapper.count(postId, userId) > 0) {
            throw new BusinessException(ResultCode.ALREADY_LIKED);
        }
        postLikeMapper.insert(postId, userId);
        postLikeMapper.increasePostLikeCount(postId);

        // 发送通知
        Post post = postMapper.selectById(postId);
        if (post != null) {
            notificationService.createNotification(post.getUserId(), userId, "LIKE", postId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlike(Long userId, Long postId) {
        validate(userId, postId);
        if (postLikeMapper.count(postId, userId) == 0) {
            throw new BusinessException(ResultCode.NOT_LIKED);
        }
        postLikeMapper.delete(postId, userId);
        postLikeMapper.decreasePostLikeCount(postId);
    }

    @Override
    public boolean isLiked(Long userId, Long postId) {
        return postLikeMapper.count(postId, userId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favorite(Long userId, Long postId) {
        validate(userId, postId);
        if (postFavoriteMapper.count(postId, userId) > 0) {
            throw new BusinessException(ResultCode.ALREADY_FAVORITED);
        }
        postFavoriteMapper.insert(postId, userId);
        postFavoriteMapper.increasePostFavoriteCount(postId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfavorite(Long userId, Long postId) {
        validate(userId, postId);
        if (postFavoriteMapper.count(postId, userId) == 0) {
            throw new BusinessException(ResultCode.NOT_FAVORITED);
        }
        postFavoriteMapper.delete(postId, userId);
        postFavoriteMapper.decreasePostFavoriteCount(postId);
    }

    @Override
    public boolean isFavorited(Long userId, Long postId) {
        return postFavoriteMapper.count(postId, userId) > 0;
    }

    @Override
    public List<PostListItemVO> getFavorites(Long userId) {
        if (userId == null)
            throw new BusinessException(ResultCode.BAD_REQUEST);
        return postFavoriteMapper.selectByUserId(userId);
    }

    @Override
    public Map<String, Boolean> getInteractionStatus(Long userId, Long postId) {
        validate(userId, postId);
        return Map.of(
                "isLiked", isLiked(userId, postId),
                "isFavorited", isFavorited(userId, postId));
    }

    private void validate(Long userId, Long postId) {
        if (userId == null || postId == null)
            throw new BusinessException(ResultCode.BAD_REQUEST);
        if (postMapper.selectById(postId) == null)
            throw new BusinessException(ResultCode.NOT_FOUND);
    }
}
