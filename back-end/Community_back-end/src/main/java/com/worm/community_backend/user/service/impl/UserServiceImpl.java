package com.worm.community_backend.user.service.impl;

import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.user.dto.LoginRequest;
import com.worm.community_backend.user.dto.RegisterRequest;
import com.worm.community_backend.user.entity.User;
import com.worm.community_backend.user.mapper.UserMapper;
import com.worm.community_backend.user.service.UserService;
import com.worm.community_backend.user.vo.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
public class UserServiceImpl implements UserService {

    private static final int USER_ID_MIN = 10000;
    private static final int USER_ID_MAX = 99999;
    private static final int USER_ID_RETRY_TIMES = 20;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse register(RegisterRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }

        String username = normalize(request.getUsername());
        String password = normalize(request.getPassword());
        String email = normalize(request.getEmail());

        validateRegisterRequest(username, password, email);
        ensureRegisterConstraint(username, email);

        User newUser = User.builder()
                .id((long) generateUniqueUserId())
                .username(username)
                .password(passwordEncoder.encode(password))
                .nickname(username)
                .email(email)
                .build();

        userMapper.insert(newUser);

        User savedUser = findUserById(newUser.getId());
        return toResponse(savedUser);
    }

    @Override
    public UserResponse login(LoginRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }

        String identifier = normalize(request.getIdentifier());
        String password = normalize(request.getPassword());
        if (identifier == null || password == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }

        User user = findUserByIdentifier(identifier);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.INVALID_CREDENTIALS);
        }

        userMapper.updateLastLoginAt(user.getId(), LocalDateTime.now());

        return toResponse(findUserById(user.getId()));
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
        User user = userMapper.selectById(id);

        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        return user;
    }

    private User findUserByIdentifier(String identifier) {
        if (identifier.matches("\\d{5}")) {
            return userMapper.selectById(Long.parseLong(identifier));
        }
        if (identifier.contains("@")) {
            return userMapper.selectByEmail(identifier);
        }
        return userMapper.selectByUsername(identifier);
    }

    private void validateRegisterRequest(String username, String password, String email) {
        if (username == null) {
            throw new BusinessException(ResultCode.USERNAME_REQUIRED);
        }
        if (password == null || password.length() < 6) {
            throw new BusinessException(ResultCode.PASSWORD_INVALID);
        }
        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException(ResultCode.EMAIL_INVALID);
        }
    }

    private void ensureRegisterConstraint(String username, String email) {
        if (userMapper.selectByUsername(username) != null) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        if (email != null && userMapper.selectByEmail(email) != null) {
            throw new BusinessException(ResultCode.EMAIL_EXISTS);
        }
    }

    private int generateUniqueUserId() {
        for (int i = 0; i < USER_ID_RETRY_TIMES; i++) {
            int candidate = ThreadLocalRandom.current().nextInt(USER_ID_MIN, USER_ID_MAX + 1);
            if (userMapper.selectById((long) candidate) == null) {
                return candidate;
            }
        }

        throw new BusinessException(ResultCode.USER_ID_GENERATE_FAILED);
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
