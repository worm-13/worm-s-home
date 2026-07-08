package com.worm.community_backend.user.service.impl;

import com.worm.community_backend.common.PageResponse;
import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.notification.service.NotificationService;
import com.worm.community_backend.user.mapper.UserFollowMapper;
import com.worm.community_backend.user.mapper.UserMapper;
import com.worm.community_backend.user.service.FollowService;
import com.worm.community_backend.user.vo.UserSimpleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final UserFollowMapper userFollowMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void follow(Long followerId, Long followingId) {
        validateFollowRequest(followerId, followingId);
        if (userFollowMapper.countByFollowerAndFollowing(followerId, followingId) > 0) {
            throw new BusinessException(1023, "already followed");
        }
        userFollowMapper.insert(followerId, followingId);
        userMapper.updateFollowingCount(followerId, 1);
        userMapper.updateFollowersCount(followingId, 1);

        // 发送通知
        notificationService.createNotification(followingId, followerId, "FOLLOW", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollow(Long followerId, Long followingId) {
        validateFollowRequest(followerId, followingId);
        if (userFollowMapper.countByFollowerAndFollowing(followerId, followingId) == 0) {
            throw new BusinessException(1024, "not followed");
        }
        userFollowMapper.delete(followerId, followingId);
        userMapper.updateFollowingCount(followerId, -1);
        userMapper.updateFollowersCount(followingId, -1);
    }

    @Override
    public boolean isFollowing(Long followerId, Long followingId) {
        return userFollowMapper.countByFollowerAndFollowing(followerId, followingId) > 0;
    }

    @Override
    public List<UserSimpleVO> getFollowing(Long userId) {
        return userFollowMapper.selectFollowing(userId);
    }

    @Override
    public List<UserSimpleVO> getFollowers(Long userId) {
        return userFollowMapper.selectFollowers(userId);
    }

    @Override
    public PageResponse<UserSimpleVO> getFollowingPage(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        long total = userFollowMapper.countFollowing(userId);
        List<UserSimpleVO> items = userFollowMapper.selectFollowingPage(userId, offset, size);
        boolean hasMore = offset + size < total;
        return new PageResponse<>(items, total, page, size, hasMore);
    }

    @Override
    public PageResponse<UserSimpleVO> getFollowersPage(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        long total = userFollowMapper.countFollowers(userId);
        List<UserSimpleVO> items = userFollowMapper.selectFollowersPage(userId, offset, size);
        boolean hasMore = offset + size < total;
        return new PageResponse<>(items, total, page, size, hasMore);
    }

    private void validateFollowRequest(Long followerId, Long followingId) {
        if (followerId == null || followingId == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        if (followerId.equals(followingId)) {
            throw new BusinessException(1025, "cannot follow yourself");
        }
        if (userMapper.selectById(followingId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
    }
}
