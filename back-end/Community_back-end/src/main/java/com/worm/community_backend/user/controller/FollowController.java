package com.worm.community_backend.user.controller;

import com.worm.community_backend.common.ApiResponse;
import com.worm.community_backend.common.PageResponse;
import com.worm.community_backend.user.service.FollowService;
import com.worm.community_backend.user.vo.UserSimpleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{id}/follow")
    public ApiResponse<Void> follow(@PathVariable Long id, Authentication auth) {
        followService.follow((Long) auth.getPrincipal(), id);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}/follow")
    public ApiResponse<Void> unfollow(@PathVariable Long id, Authentication auth) {
        followService.unfollow((Long) auth.getPrincipal(), id);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/following")
    public ApiResponse<PageResponse<UserSimpleVO>> getFollowing(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(followService.getFollowingPage(id, page, size));
    }

    @GetMapping("/{id}/followers")
    public ApiResponse<PageResponse<UserSimpleVO>> getFollowers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(followService.getFollowersPage(id, page, size));
    }

    @GetMapping("/{id}/follow-status")
    public ApiResponse<Map<String, Boolean>> getFollowStatus(@PathVariable Long id, Authentication auth) {
        Long currentUserId = (Long) auth.getPrincipal();
        boolean isFollowing = followService.isFollowing(currentUserId, id);
        boolean isFollowedBy = followService.isFollowing(id, currentUserId);
        return ApiResponse.success(Map.of("isFollowing", isFollowing, "isFollowedBy", isFollowedBy));
    }
}
