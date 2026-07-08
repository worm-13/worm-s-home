package com.worm.community_backend.user.service.impl;

import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.config.JwtTokenProvider;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.user.dto.ChangePasswordRequest;
import com.worm.community_backend.user.dto.LoginRequest;
import com.worm.community_backend.user.dto.RegisterRequest;
import com.worm.community_backend.user.entity.User;
import com.worm.community_backend.user.mapper.UserMapper;
import com.worm.community_backend.user.service.RefreshTokenService;
import com.worm.community_backend.user.vo.AuthResponse;
import com.worm.community_backend.user.vo.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RefreshTokenService refreshTokenService;

    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() throws IOException {
        Path tempDir = Files.createTempDirectory("usertest");
        userService = new UserServiceImpl(
                userMapper, passwordEncoder, jwtTokenProvider, refreshTokenService,
                tempDir.resolve("avatars").toString(), "/avatars", 20_971_520L,
                tempDir.resolve("backgrounds").toString(), "/backgrounds", 20_971_520L
        );
        testUser = User.builder()
                .id(12345L)
                .username("testuser")
                .password("encodedPassword")
                .nickname("testuser")
                .email("test@example.com")
                .avatar(null)
                .backgroundImage(null)
                .bio(null)
                .gender(0)
                .birthday(null)
                .location(null)
                .followersCount(0)
                .followingCount(0)
                .postsCount(0)
                .status(1)
                .role("USER")
                .lastLoginAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("password123");
        request.setEmail("new@example.com");

        when(userMapper.selectByUsername("newuser")).thenReturn(null);
        when(userMapper.selectByEmail("new@example.com")).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userMapper.insert(any(User.class))).thenReturn(1);
        // First call: generateUniqueUserId checks if ID exists (returns null = available)
        // Second call: findUserById after insert (returns testUser)
        when(userMapper.selectById(anyLong())).thenReturn(null).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString())).thenReturn("accessToken");
        when(refreshTokenService.createRefreshToken(anyLong())).thenReturn("refreshToken");
        when(jwtTokenProvider.getExpirationSeconds()).thenReturn(604800L);

        AuthResponse response = userService.register(request);

        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
        assertEquals("refreshToken", response.getRefreshToken());
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void register_UsernameExists() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setPassword("password123");

        when(userMapper.selectByUsername("existinguser")).thenReturn(testUser);

        assertThrows(BusinessException.class, () -> userService.register(request));
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("testuser");
        request.setPassword("password123");

        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(userMapper.selectById(12345L)).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(12345L, "USER")).thenReturn("accessToken");
        when(refreshTokenService.createRefreshToken(12345L)).thenReturn("refreshToken");
        when(jwtTokenProvider.getExpirationSeconds()).thenReturn(604800L);

        AuthResponse response = userService.login(request);

        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
        verify(userMapper).updateLastLoginAt(eq(12345L), any(LocalDateTime.class));
    }

    @Test
    void login_InvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setIdentifier("testuser");
        request.setPassword("wrongpassword");

        when(userMapper.selectByUsername("testuser")).thenReturn(testUser);
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        assertThrows(BusinessException.class, () -> userService.login(request));
    }

    @Test
    void getUserById_Success() {
        when(userMapper.selectById(12345L)).thenReturn(testUser);

        UserResponse response = userService.getUserById(12345L);

        assertNotNull(response);
        assertEquals(12345L, response.getId());
        assertEquals("testuser", response.getUsername());
    }

    @Test
    void getUserById_NotFound() {
        when(userMapper.selectById(99999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> userService.getUserById(99999L));
    }

    @Test
    void changePassword_Success() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("oldPassword");
        request.setNewPassword("newPassword");

        when(userMapper.selectById(12345L)).thenReturn(testUser);
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");
        when(userMapper.updatePasswordById(12345L, "newEncodedPassword")).thenReturn(1);

        assertDoesNotThrow(() -> userService.changePassword(12345L, request));
        verify(userMapper).updatePasswordById(12345L, "newEncodedPassword");
    }

    @Test
    void changePassword_WrongOldPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("wrongPassword");
        request.setNewPassword("newPassword");

        when(userMapper.selectById(12345L)).thenReturn(testUser);
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThrows(BusinessException.class, () -> userService.changePassword(12345L, request));
    }

    @Test
    void refreshToken_Success() {
        when(refreshTokenService.validateRefreshToken("refreshToken")).thenReturn(12345L);
        when(userMapper.selectById(12345L)).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(12345L, "USER")).thenReturn("newAccessToken");
        when(refreshTokenService.createRefreshToken(12345L)).thenReturn("newRefreshToken");
        when(jwtTokenProvider.getExpirationSeconds()).thenReturn(604800L);

        AuthResponse response = userService.refreshToken("refreshToken");

        assertNotNull(response);
        assertEquals("newAccessToken", response.getAccessToken());
        verify(refreshTokenService).revokeRefreshToken("refreshToken");
    }

    @Test
    void logout_Success() {
        assertDoesNotThrow(() -> userService.logout(12345L));
        verify(refreshTokenService).revokeAllUserTokens(12345L);
    }
}
