package com.worm.community_backend.user.service.impl;

import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.user.dto.CreateUserRequest;
import com.worm.community_backend.user.entity.User;
import com.worm.community_backend.user.service.UserService;
import com.worm.community_backend.user.vo.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserServiceImpl implements UserService {

    private final Map<Long, User> userStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1L);

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        if (request == null || !StringUtils.hasText(request.getUsername())) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }

        Long userId = idGenerator.getAndIncrement();
        User user = User.builder()
                .id(userId)
                .username(request.getUsername().trim())
                .build();
        userStore.put(userId, user);

        return new UserResponse(user.getId(), user.getUsername());
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = userStore.get(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        return new UserResponse(user.getId(), user.getUsername());
    }
}

