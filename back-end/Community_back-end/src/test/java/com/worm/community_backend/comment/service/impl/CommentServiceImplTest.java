package com.worm.community_backend.comment.service.impl;

import com.worm.community_backend.comment.dto.CommentCreateDTO;
import com.worm.community_backend.comment.entity.Comment;
import com.worm.community_backend.comment.mapper.CommentMapper;
import com.worm.community_backend.comment.vo.CommentVO;
import com.worm.community_backend.common.PageResponse;
import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.moderation.service.SensitiveWordFilter;
import com.worm.community_backend.notification.service.NotificationService;
import com.worm.community_backend.post.entity.Post;
import com.worm.community_backend.post.mapper.PostMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentMapper commentMapper;
    @Mock
    private PostMapper postMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private SensitiveWordFilter sensitiveWordFilter;

    @InjectMocks
    private CommentServiceImpl commentService;

    private Post testPost;
    private Comment testComment;

    @BeforeEach
    void setUp() {
        testPost = new Post();
        testPost.setId(1L);
        testPost.setUserId(100L);
        testPost.setTitle("Test Post");
        testPost.setContent("Test Content");
        testPost.setStatus(1);

        testComment = new Comment();
        testComment.setId(1L);
        testComment.setPostId(1L);
        testComment.setUserId(200L);
        testComment.setContent("Test Comment");
        testComment.setStatus(1);
        testComment.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void createComment_Success() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setPostId(1L);
        dto.setContent("Nice post!");

        when(postMapper.selectById(1L)).thenReturn(testPost);
        when(sensitiveWordFilter.containsSensitiveWord("Nice post!")).thenReturn(false);
        doAnswer(invocation -> {
            Comment c = invocation.getArgument(0);
            c.setId(1L);
            return 1;
        }).when(commentMapper).insert(any(Comment.class));
        when(commentMapper.increasePostCommentCount(1L)).thenReturn(1);

        Long commentId = commentService.createComment(200L, dto);

        assertNotNull(commentId);
        verify(commentMapper).insert(any(Comment.class));
        verify(commentMapper).increasePostCommentCount(1L);
        verify(notificationService).createNotification(eq(100L), eq(200L), eq("COMMENT"), eq(1L));
    }

    @Test
    void createComment_BlankContent() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setPostId(1L);
        dto.setContent("   ");

        assertThrows(BusinessException.class, () -> commentService.createComment(200L, dto));
    }

    @Test
    void createComment_ContentTooLong() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setPostId(1L);
        dto.setContent("a".repeat(501));

        assertThrows(BusinessException.class, () -> commentService.createComment(200L, dto));
    }

    @Test
    void createComment_SensitiveWord() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setPostId(1L);
        dto.setContent("This contains spam");

        when(sensitiveWordFilter.containsSensitiveWord("This contains spam")).thenReturn(true);

        assertThrows(BusinessException.class, () -> commentService.createComment(200L, dto));
    }

    @Test
    void createComment_PostNotFound() {
        CommentCreateDTO dto = new CommentCreateDTO();
        dto.setPostId(999L);
        dto.setContent("Nice post!");

        when(postMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> commentService.createComment(200L, dto));
    }

    @Test
    void deleteComment_Success() {
        when(commentMapper.selectById(1L)).thenReturn(testComment);
        when(commentMapper.softDelete(1L)).thenReturn(1);
        when(commentMapper.decreasePostCommentCount(1L)).thenReturn(1);

        assertDoesNotThrow(() -> commentService.deleteComment(200L, 1L));
        verify(commentMapper).softDelete(1L);
    }

    @Test
    void deleteComment_NotOwner() {
        when(commentMapper.selectById(1L)).thenReturn(testComment);

        assertThrows(BusinessException.class, () -> commentService.deleteComment(999L, 1L));
    }

    @Test
    void getCommentsByPostIdPage_Success() {
        CommentVO commentVO = new CommentVO();
        commentVO.setId(1L);
        commentVO.setContent("Test Comment");

        when(commentMapper.countByPostId(1L)).thenReturn(1L);
        when(commentMapper.selectByPostIdPage(1L, 0, 20)).thenReturn(Arrays.asList(commentVO));

        PageResponse<CommentVO> response = commentService.getCommentsByPostIdPage(1L, 1, 20);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals(1L, response.getTotal());
    }
}
