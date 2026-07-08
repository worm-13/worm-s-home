package com.worm.community_backend.user.service.impl;

import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.config.JwtTokenProvider;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.user.entity.User;
import com.worm.community_backend.user.mapper.UserMapper;
import com.worm.community_backend.user.service.RefreshTokenService;
import com.worm.community_backend.user.vo.UserResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplBackgroundImageTests {

    @TempDir
    Path tempDir;

    @Test
    void shouldUploadBackgroundImageAndPersistUrl() {
        UserMapper userMapper = mock(UserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);

        User existingUser = User.builder().id(10001L).username("alice").build();
        User updatedUser = User.builder()
                .id(10001L)
                .username("alice")
                .backgroundImage("/backgrounds/new.png")
                .build();

        when(userMapper.selectById(10001L)).thenReturn(existingUser, updatedUser);
        when(userMapper.updateBackgroundImageById(anyLong(), anyString())).thenReturn(1);

        UserServiceImpl service = new UserServiceImpl(
                userMapper,
                passwordEncoder,
                jwtTokenProvider,
                refreshTokenService,
                tempDir.resolve("avatars").toString(),
                "/avatars",
                5 * 1024 * 1024,
                tempDir.resolve("backgrounds").toString(),
                "/backgrounds",
                5 * 1024 * 1024
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cover.png",
                "image/png",
                "png-content".getBytes()
        );

        UserResponse response = service.uploadBackgroundImage(10001L, file);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(userMapper, times(1)).updateBackgroundImageById(anyLong(), urlCaptor.capture());
        String savedUrl = urlCaptor.getValue();

        Assertions.assertNotNull(response);
        Assertions.assertTrue(savedUrl.startsWith("/backgrounds/"));
        Assertions.assertTrue(savedUrl.endsWith(".png"));
        Assertions.assertEquals(savedUrl, response.getBackgroundImage());
    }

    @Test
    void shouldRejectInvalidBackgroundType() {
        UserMapper userMapper = mock(UserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);

        when(userMapper.selectById(10001L)).thenReturn(User.builder().id(10001L).username("alice").build());

        UserServiceImpl service = new UserServiceImpl(
                userMapper,
                passwordEncoder,
                jwtTokenProvider,
                refreshTokenService,
                tempDir.resolve("avatars").toString(),
                "/avatars",
                5 * 1024 * 1024,
                tempDir.resolve("backgrounds").toString(),
                "/backgrounds",
                5 * 1024 * 1024
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cover.txt",
                "text/plain",
                "not-image".getBytes()
        );

        BusinessException ex = Assertions.assertThrows(BusinessException.class,
                () -> service.uploadBackgroundImage(10001L, file));

        Assertions.assertEquals(ResultCode.BACKGROUND_TYPE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void shouldRejectBackgroundFileWhenSizeExceeded() {
        UserMapper userMapper = mock(UserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);

        when(userMapper.selectById(10001L)).thenReturn(User.builder().id(10001L).username("alice").build());

        UserServiceImpl service = new UserServiceImpl(
                userMapper,
                passwordEncoder,
                jwtTokenProvider,
                refreshTokenService,
                tempDir.resolve("avatars").toString(),
                "/avatars",
                5 * 1024 * 1024,
                tempDir.resolve("backgrounds").toString(),
                "/backgrounds",
                1024
        );

        byte[] oversizedBytes = new byte[2048];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cover.png",
                "image/png",
                oversizedBytes
        );

        BusinessException ex = Assertions.assertThrows(BusinessException.class,
                () -> service.uploadBackgroundImage(10001L, file));

        Assertions.assertEquals(ResultCode.BACKGROUND_SIZE_EXCEEDED.getCode(), ex.getCode());
    }
}
