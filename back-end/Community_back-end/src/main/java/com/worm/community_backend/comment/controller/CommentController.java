package com.worm.community_backend.comment.controller;

import com.worm.community_backend.comment.dto.CommentCreateDTO;
import com.worm.community_backend.comment.service.CommentService;
import com.worm.community_backend.comment.vo.CommentVO;
import com.worm.community_backend.common.ApiResponse;
import com.worm.community_backend.common.PageResponse;
import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody CommentCreateDTO dto, Authentication auth) {
        Long id = commentService.createComment(getUserId(auth), dto);
        return ApiResponse.success(Map.of("commentId", id));
    }

    @GetMapping("/post/{postId}")
    public ApiResponse<PageResponse<CommentVO>> listByPost(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(commentService.getCommentsByPostIdPage(postId, page, size));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, Authentication auth) {
        commentService.deleteComment(getUserId(auth), id);
        return ApiResponse.success(null);
    }

    private Long getUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        return userId;
    }
}
