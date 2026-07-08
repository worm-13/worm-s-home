package com.worm.community_backend.comment.service;

import com.worm.community_backend.comment.dto.CommentCreateDTO;
import com.worm.community_backend.comment.vo.CommentVO;
import com.worm.community_backend.common.PageResponse;

import java.util.List;

public interface CommentService {
    Long createComment(Long userId, CommentCreateDTO dto);

    List<CommentVO> getCommentsByPostId(Long postId);

    void deleteComment(Long userId, Long commentId);

    PageResponse<CommentVO> getCommentsByPostIdPage(Long postId, int page, int size);
}
