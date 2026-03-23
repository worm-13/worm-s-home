package com.worm.community_backend.user.service.impl;

import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.user.dto.CreateUserRequest;
import com.worm.community_backend.user.entity.User;
import com.worm.community_backend.user.mapper.UserMapper;
import com.worm.community_backend.user.service.UserService;
import com.worm.community_backend.user.vo.UserResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // Keeps local run/debug usable before datasource is configured.
    private final Map<Long, User> userStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1L);

    public UserServiceImpl(ObjectProvider<UserMapper> userMapperProvider, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapperProvider.getIfAvailable();
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        if (request == null || !StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }

        User newUser = User.builder()
                .username(request.getUsername().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(normalize(request.getNickname()))
                .email(normalize(request.getEmail()))
                .build();

        if (userMapper != null) {
            try {
                userMapper.insert(newUser);
            } catch (DataIntegrityViolationException ex) {
                throw new BusinessException(ResultCode.BAD_REQUEST);
            }
        } else {
            Long userId = idGenerator.getAndIncrement();
            newUser.setId(userId);
            userStore.put(userId, newUser);
        }

        User savedUser = findUserById(newUser.getId());
        return toResponse(savedUser);
    }

    @Override
    public UserResponse getUserById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }

        User user = findUserById(id);
        return toResponse(user);
    }

    private User findUserById(Long id) {
        User user = null;
        if (userMapper != null) {
            user = userMapper.selectById(id);
        }
        if (user == null) {
            user = userStore.get(id);
        }

        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        return user;
    }

    private static String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getAvatar(),
                user.getBackgroundImage(),
                user.getBio(),
                user.getGender(),
                user.getBirthday(),
                user.getLocation(),
                user.getFollowersCount(),
                user.getFollowingCount(),
                user.getPostsCount(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}
