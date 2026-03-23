package com.worm.community_backend.user.service;

import com.worm.community_backend.user.dto.CreateUserRequest;
import com.worm.community_backend.user.vo.UserResponse;

public interface UserService {
    UserResponse createUser(CreateUserRequest request);

    UserResponse getUserById(Long id);
}

