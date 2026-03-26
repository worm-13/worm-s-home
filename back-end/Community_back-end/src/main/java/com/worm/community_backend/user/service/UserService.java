package com.worm.community_backend.user.service;

import com.worm.community_backend.user.dto.LoginRequest;
import com.worm.community_backend.user.dto.RegisterRequest;
import com.worm.community_backend.user.vo.UserResponse;

public interface UserService {
    UserResponse register(RegisterRequest request);

    UserResponse login(LoginRequest request);

    UserResponse getUserById(Long id);
}

