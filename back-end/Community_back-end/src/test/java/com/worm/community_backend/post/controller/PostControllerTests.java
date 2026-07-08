package com.worm.community_backend.post.controller;

import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.post.dto.PostCreateDTO;
import com.worm.community_backend.post.service.PostService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PostControllerTests {

    private PostService postService;
    private PostController controller;

    @BeforeEach
    void setUp() throws Exception {
        postService = mock(PostService.class);
        controller = new PostController(postService);
        setField(controller, "postCoverUploadDir", "uploads/post-covers");
        setField(controller, "postCoverPublicPath", "/post-covers");
        setField(controller, "postCoverMaxSizeBytes", 20_971_520L);
    }

    @Test
    void shouldUseAuthenticatedPrincipalForCreatePost() {
        when(postService.createPost(eq(10001L), any(PostCreateDTO.class))).thenReturn(20001L);

        PostCreateDTO createDTO = new PostCreateDTO();
        createDTO.setTitle("My first post");
        createDTO.setContent("Hello world");

        Authentication auth = new UsernamePasswordAuthenticationToken(10001L, null, Collections.emptyList());

        var response = controller.createPost(createDTO, auth);

        Assertions.assertEquals(ResultCode.SUCCESS.getCode(), response.getCode());
        Assertions.assertEquals(20001L, response.getData().get("postId"));

        ArgumentCaptor<Long> userIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<PostCreateDTO> dtoCaptor = ArgumentCaptor.forClass(PostCreateDTO.class);
        verify(postService).createPost(userIdCaptor.capture(), dtoCaptor.capture());
        Assertions.assertEquals(10001L, userIdCaptor.getValue());
        Assertions.assertEquals("My first post", dtoCaptor.getValue().getTitle());
        Assertions.assertEquals("Hello world", dtoCaptor.getValue().getContent());
    }

    @Test
    void shouldRejectCreatePostWhenAuthenticationIsMissing() {
        PostCreateDTO createDTO = new PostCreateDTO();
        createDTO.setTitle("My first post");
        createDTO.setContent("Hello world");

        Assertions.assertThrows(
                com.worm.community_backend.exception.BusinessException.class,
                () -> controller.createPost(createDTO, null));
        verifyNoInteractions(postService);
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
