package com.worm.community_backend.interaction.service;

import com.worm.community_backend.post.vo.PostListItemVO;

import java.util.List;
import java.util.Map;

public interface InteractionService {
    void like(Long userId, Long postId);
    void unlike(Long userId, Long postId);
    boolean isLiked(Long userId, Long postId);
    void favorite(Long userId, Long postId);
    void unfavorite(Long userId, Long postId);
    boolean isFavorited(Long userId, Long postId);
    List<PostListItemVO> getFavorites(Long userId);
    Map<String, Boolean> getInteractionStatus(Long userId, Long postId);
}
