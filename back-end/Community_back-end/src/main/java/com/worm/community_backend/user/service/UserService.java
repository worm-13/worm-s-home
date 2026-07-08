package com.worm.community_backend.user.service;

import com.worm.community_backend.user.dto.ChangePasswordRequest;
import com.worm.community_backend.user.dto.LoginRequest;
import com.worm.community_backend.user.dto.RegisterRequest;
import com.worm.community_backend.user.dto.UpdateProfileRequest;
import com.worm.community_backend.user.vo.AuthResponse;
import com.worm.community_backend.user.vo.UserResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(String refreshToken);
    void logout(Long userId);
    UserResponse getUserById(Long id);
    UserResponse uploadAvatar(Long userId, MultipartFile file);
    UserResponse uploadBackgroundImage(Long userId, MultipartFile file);
    UserResponse updateProfile(Long userId, UpdateProfileRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);
}

