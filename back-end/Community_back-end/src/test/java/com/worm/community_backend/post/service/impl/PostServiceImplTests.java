package com.worm.community_backend.post.service.impl;

import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.interaction.service.InteractionService;
import com.worm.community_backend.moderation.service.SensitiveWordFilter;
import com.worm.community_backend.post.dto.PostCreateDTO;
import com.worm.community_backend.post.entity.Post;
import com.worm.community_backend.post.mapper.PostMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostServiceImplTests {

    @Test
    void shouldCreatePostAndGenerateSummaryWhenMissing() {
        PostMapper postMapper = mock(PostMapper.class);
        InteractionService interactionService = mock(InteractionService.class);
        SensitiveWordFilter sensitiveWordFilter = mock(SensitiveWordFilter.class);
        PostServiceImpl service = new PostServiceImpl(postMapper, interactionService, sensitiveWordFilter);

        doAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            post.setId(20001L);
            return 1;
        }).when(postMapper).insertPost(any(Post.class));
        when(postMapper.increaseUserPostsCount(10001L)).thenReturn(1);

        PostCreateDTO dto = new PostCreateDTO();
        dto.setTitle("My first post");
        dto.setContent("a".repeat(130));

        Long postId = service.createPost(10001L, dto);

        Assertions.assertEquals(20001L, postId);

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        verify(postMapper).insertPost(captor.capture());
        Post insertedPost = captor.getValue();
        Assertions.assertEquals(100, insertedPost.getSummary().length());
        Assertions.assertEquals("My first post", insertedPost.getTitle());
    }

    @Test
    void shouldRejectWhenTitleIsBlank() {
        PostMapper postMapper = mock(PostMapper.class);
        InteractionService interactionService = mock(InteractionService.class);
        SensitiveWordFilter sensitiveWordFilter = mock(SensitiveWordFilter.class);
        PostServiceImpl service = new PostServiceImpl(postMapper, interactionService, sensitiveWordFilter);

        PostCreateDTO dto = new PostCreateDTO();
        dto.setTitle("   ");
        dto.setContent("content");

        BusinessException ex = Assertions.assertThrows(BusinessException.class,
                () -> service.createPost(10001L, dto));

        Assertions.assertEquals(ResultCode.POST_TITLE_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void shouldFailWhenUserPostsCountUpdateFails() {
        PostMapper postMapper = mock(PostMapper.class);
        InteractionService interactionService = mock(InteractionService.class);
        SensitiveWordFilter sensitiveWordFilter = mock(SensitiveWordFilter.class);
        PostServiceImpl service = new PostServiceImpl(postMapper, interactionService, sensitiveWordFilter);

        doAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            post.setId(20002L);
            return 1;
        }).when(postMapper).insertPost(any(Post.class));
        when(postMapper.increaseUserPostsCount(anyLong())).thenReturn(0);

        PostCreateDTO dto = new PostCreateDTO();
        dto.setTitle("Title");
        dto.setContent("Markdown content");

        BusinessException ex = Assertions.assertThrows(BusinessException.class,
                () -> service.createPost(10001L, dto));

        Assertions.assertEquals(ResultCode.POST_CREATE_FAILED.getCode(), ex.getCode());
    }
}

