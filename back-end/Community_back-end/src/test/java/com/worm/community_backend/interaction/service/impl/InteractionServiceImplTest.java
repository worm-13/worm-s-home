package com.worm.community_backend.interaction.service.impl;

import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.interaction.mapper.PostFavoriteMapper;
import com.worm.community_backend.interaction.mapper.PostLikeMapper;
import com.worm.community_backend.notification.service.NotificationService;
import com.worm.community_backend.post.entity.Post;
import com.worm.community_backend.post.mapper.PostMapper;
import com.worm.community_backend.post.vo.PostListItemVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InteractionServiceImplTest {

    @Mock
    private PostLikeMapper postLikeMapper;
    @Mock
    private PostFavoriteMapper postFavoriteMapper;
    @Mock
    private PostMapper postMapper;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private InteractionServiceImpl interactionService;

    private Post testPost;
    private final Long userId = 200L;
    private final Long postId = 1L;
    private final Long postOwnerId = 100L;

    @BeforeEach
    void setUp() {
        testPost = new Post();
        testPost.setId(postId);
        testPost.setUserId(postOwnerId);
        testPost.setTitle("Test Post");
        testPost.setContent("Test Content");
        testPost.setStatus(1);
    }

    @Test
    void like_Success() {
        when(postMapper.selectById(postId)).thenReturn(testPost);
        when(postLikeMapper.count(postId, userId)).thenReturn(0);

        interactionService.like(userId, postId);

        verify(postLikeMapper).insert(postId, userId);
        verify(postLikeMapper).increasePostLikeCount(postId);
        verify(notificationService).createNotification(postOwnerId, userId, "LIKE", postId);
    }

    @Test
    void like_AlreadyLiked() {
        when(postMapper.selectById(postId)).thenReturn(testPost);
        when(postLikeMapper.count(postId, userId)).thenReturn(1);

        assertThrows(BusinessException.class, () -> interactionService.like(userId, postId));
    }

    @Test
    void like_PostNotFound() {
        when(postMapper.selectById(postId)).thenReturn(null);

        assertThrows(BusinessException.class, () -> interactionService.like(userId, postId));
    }

    @Test
    void like_NullParams() {
        assertThrows(BusinessException.class, () -> interactionService.like(null, postId));
        assertThrows(BusinessException.class, () -> interactionService.like(userId, null));
    }

    @Test
    void unlike_Success() {
        when(postMapper.selectById(postId)).thenReturn(testPost);
        when(postLikeMapper.count(postId, userId)).thenReturn(1);

        interactionService.unlike(userId, postId);

        verify(postLikeMapper).delete(postId, userId);
        verify(postLikeMapper).decreasePostLikeCount(postId);
    }

    @Test
    void unlike_NotLiked() {
        when(postMapper.selectById(postId)).thenReturn(testPost);
        when(postLikeMapper.count(postId, userId)).thenReturn(0);

        assertThrows(BusinessException.class, () -> interactionService.unlike(userId, postId));
    }

    @Test
    void favorite_Success() {
        when(postMapper.selectById(postId)).thenReturn(testPost);
        when(postFavoriteMapper.count(postId, userId)).thenReturn(0);

        interactionService.favorite(userId, postId);

        verify(postFavoriteMapper).insert(postId, userId);
        verify(postFavoriteMapper).increasePostFavoriteCount(postId);
    }

    @Test
    void favorite_AlreadyFavorited() {
        when(postMapper.selectById(postId)).thenReturn(testPost);
        when(postFavoriteMapper.count(postId, userId)).thenReturn(1);

        assertThrows(BusinessException.class, () -> interactionService.favorite(userId, postId));
    }

    @Test
    void unfavorite_Success() {
        when(postMapper.selectById(postId)).thenReturn(testPost);
        when(postFavoriteMapper.count(postId, userId)).thenReturn(1);

        interactionService.unfavorite(userId, postId);

        verify(postFavoriteMapper).delete(postId, userId);
        verify(postFavoriteMapper).decreasePostFavoriteCount(postId);
    }

    @Test
    void unfavorite_NotFavorited() {
        when(postMapper.selectById(postId)).thenReturn(testPost);
        when(postFavoriteMapper.count(postId, userId)).thenReturn(0);

        assertThrows(BusinessException.class, () -> interactionService.unfavorite(userId, postId));
    }

    @Test
    void isLiked_True() {
        when(postLikeMapper.count(postId, userId)).thenReturn(1);

        assertTrue(interactionService.isLiked(userId, postId));
    }

    @Test
    void isLiked_False() {
        when(postLikeMapper.count(postId, userId)).thenReturn(0);

        assertFalse(interactionService.isLiked(userId, postId));
    }

    @Test
    void isFavorited_True() {
        when(postFavoriteMapper.count(postId, userId)).thenReturn(1);

        assertTrue(interactionService.isFavorited(userId, postId));
    }

    @Test
    void isFavorited_False() {
        when(postFavoriteMapper.count(postId, userId)).thenReturn(0);

        assertFalse(interactionService.isFavorited(userId, postId));
    }

    @Test
    void getInteractionStatus_Success() {
        when(postMapper.selectById(postId)).thenReturn(testPost);
        when(postLikeMapper.count(postId, userId)).thenReturn(1);
        when(postFavoriteMapper.count(postId, userId)).thenReturn(0);

        Map<String, Boolean> status = interactionService.getInteractionStatus(userId, postId);

        assertTrue(status.get("isLiked"));
        assertFalse(status.get("isFavorited"));
    }

    @Test
    void getFavorites_Success() {
        PostListItemVO vo = new PostListItemVO();
        vo.setId(postId);
        vo.setTitle("Test Post");

        when(postFavoriteMapper.selectByUserId(userId)).thenReturn(Arrays.asList(vo));

        List<PostListItemVO> favorites = interactionService.getFavorites(userId);

        assertNotNull(favorites);
        assertEquals(1, favorites.size());
        assertEquals(postId, favorites.get(0).getId());
    }

    @Test
    void getFavorites_NullUserId() {
        assertThrows(BusinessException.class, () -> interactionService.getFavorites(null));
    }
}
