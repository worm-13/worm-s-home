package com.worm.community_backend.interaction.controller;

import com.worm.community_backend.common.ApiResponse;
import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.interaction.service.InteractionService;
import com.worm.community_backend.post.vo.PostListItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts/{postId}")
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    @PostMapping("/like")
    public ApiResponse<Void> like(@PathVariable Long postId, Authentication auth) {
        interactionService.like(getUserId(auth), postId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/like")
    public ApiResponse<Void> unlike(@PathVariable Long postId, Authentication auth) {
        interactionService.unlike(getUserId(auth), postId);
        return ApiResponse.success(null);
    }

    @PostMapping("/favorite")
    public ApiResponse<Void> favorite(@PathVariable Long postId, Authentication auth) {
        interactionService.favorite(getUserId(auth), postId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/favorite")
    public ApiResponse<Void> unfavorite(@PathVariable Long postId, Authentication auth) {
        interactionService.unfavorite(getUserId(auth), postId);
        return ApiResponse.success(null);
    }

    @GetMapping("/interaction-status")
    public ApiResponse<Map<String, Boolean>> getStatus(@PathVariable Long postId, Authentication auth) {
        return ApiResponse.success(interactionService.getInteractionStatus(getUserId(auth), postId));
    }

    private Long getUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        return userId;
    }
}
