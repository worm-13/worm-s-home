package com.worm.community_backend.user.service;

import com.worm.community_backend.common.PageResponse;
import com.worm.community_backend.user.vo.UserSimpleVO;
import java.util.List;

public interface FollowService {
    void follow(Long followerId, Long followingId);
    void unfollow(Long followerId, Long followingId);
    boolean isFollowing(Long followerId, Long followingId);
    List<UserSimpleVO> getFollowing(Long userId);
    List<UserSimpleVO> getFollowers(Long userId);
    PageResponse<UserSimpleVO> getFollowingPage(Long userId, int page, int size);
    PageResponse<UserSimpleVO> getFollowersPage(Long userId, int page, int size);
}
