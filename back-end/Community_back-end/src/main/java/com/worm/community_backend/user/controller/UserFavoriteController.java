package com.worm.community_backend.user.controller;

import com.worm.community_backend.common.ApiResponse;
import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.interaction.service.InteractionService;
import com.worm.community_backend.post.vo.PostListItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserFavoriteController {

    private final InteractionService interactionService;

    @GetMapping("/favorites")
    public ApiResponse<List<PostListItemVO>> getMyFavorites(Authentication auth) {
        Long userId = getUserId(auth);
        return ApiResponse.success(interactionService.getFavorites(userId));
    }

    private Long getUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        return userId;
    }
}
