package com.worm.community_backend.user.service.impl;

import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.config.JwtTokenProvider;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.user.dto.ChangePasswordRequest;
import com.worm.community_backend.user.dto.LoginRequest;
import com.worm.community_backend.user.dto.RegisterRequest;
import com.worm.community_backend.user.dto.UpdateProfileRequest;
import com.worm.community_backend.user.entity.User;
import com.worm.community_backend.user.mapper.UserMapper;
import com.worm.community_backend.user.service.RefreshTokenService;
import com.worm.community_backend.user.service.UserService;
import com.worm.community_backend.user.vo.AuthResponse;
import com.worm.community_backend.user.vo.UserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 用户业务实现：注册登录、资料查询、头像与背景图上传。
 */
@Service
public class UserServiceImpl implements UserService {

    private static final int USER_ID_MIN = 10000;
    private static final int USER_ID_MAX = 99999;
    private static final int USER_ID_RETRY_TIMES = 20;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Set<String> ALLOWED_AVATAR_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final Path avatarUploadPath;
    private final String avatarPublicPath;
    private final long avatarMaxSizeBytes;
    private final Path backgroundUploadPath;
    private final String backgroundPublicPath;
    private final long backgroundMaxSizeBytes;

    public UserServiceImpl(UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService,
            @Value("${app.avatar.upload-dir:uploads/avatars}") String avatarUploadDir,
            @Value("${app.avatar.public-path:/avatars}") String avatarPublicPath,
            @Value("${app.avatar.max-size-bytes:20971520}") long avatarMaxSizeBytes,
            @Value("${app.background.upload-dir:uploads/backgrounds}") String backgroundUploadDir,
            @Value("${app.background.public-path:/backgrounds}") String backgroundPublicPath,
            @Value("${app.background.max-size-bytes:20971520}") long backgroundMaxSizeBytes) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.avatarUploadPath = Paths.get(avatarUploadDir).toAbsolutePath().normalize();
        this.avatarPublicPath = normalizePublicPath(avatarPublicPath);
        this.avatarMaxSizeBytes = avatarMaxSizeBytes;
        this.backgroundUploadPath = Paths.get(backgroundUploadDir).toAbsolutePath().normalize();
        this.backgroundPublicPath = normalizePublicPath(backgroundPublicPath);
        this.backgroundMaxSizeBytes = backgroundMaxSizeBytes;
    }

    /**
     * 注册用户并返回包含 JWT 的登录响应。
     */
    @Override
    public AuthResponse register(RegisterRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }

        String username = normalize(request.getUsername());
        String password = normalize(request.getPassword());
        String email = normalize(request.getEmail());

        ensureRegisterConstraint(username, email);

        User newUser = User.builder()
                // 项目使用 5 位数字作为用户 ID。
                .id((long) generateUniqueUserId())
                .username(username)
                .password(passwordEncoder.encode(password))
                .nickname(username)
                .email(email)
                .role("USER")
                .build();

        userMapper.insert(newUser);

        User savedUser = findUserById(newUser.getId());
        return toAuthResponse(savedUser);
    }

    /**
     * 支持用户 ID / 用户名 / 邮箱三种标识登录。
     */
    @Override
    public AuthResponse login(LoginRequest request) {
        if (request == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }

        String identifier = normalize(request.getIdentifier());
        String password = normalize(request.getPassword());

        User user = findUserByIdentifier(identifier);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.INVALID_CREDENTIALS);
        }

        userMapper.updateLastLoginAt(user.getId(), LocalDateTime.now());

        return toAuthResponse(findUserById(user.getId()));
    }

    /**
     * 使用刷新令牌获取新的访问令牌。
     */
    @Override
    public AuthResponse refreshToken(String refreshToken) {
        Long userId = refreshTokenService.validateRefreshToken(refreshToken);
        User user = findUserById(userId);
        
        // 撤销旧的刷新令牌
        refreshTokenService.revokeRefreshToken(refreshToken);
        
        return toAuthResponse(user);
    }

    /**
     * 用户登出，撤销所有刷新令牌。
     */
    @Override
    public void logout(Long userId) {
        refreshTokenService.revokeAllUserTokens(userId);
    }

    @Override
    @Cacheable(value = "userProfiles", key = "#id")
    public UserResponse getUserById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }

        User user = findUserById(id);
        return toResponse(user);
    }

    /**
     * 上传用户头像并更新 users.avatar 字段。
     */
    @Override
    public UserResponse uploadAvatar(Long userId, MultipartFile file) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.AVATAR_FILE_REQUIRED);
        }
        if (file.getSize() > avatarMaxSizeBytes) {
            throw new BusinessException(ResultCode.AVATAR_SIZE_EXCEEDED);
        }

        User user = findUserById(userId);
        String extension = resolveAvatarExtension(file);
        String fileName = buildAvatarFileName(userId, extension);
        Path target = avatarUploadPath.resolve(fileName).normalize();

        // 防止路径穿越攻击。
        if (!target.startsWith(avatarUploadPath)) {
            throw new BusinessException(ResultCode.AVATAR_UPLOAD_FAILED);
        }

        try {
            Files.createDirectories(avatarUploadPath);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.AVATAR_UPLOAD_FAILED);
        }

        String avatarUrl = avatarPublicPath + "/" + fileName;
        int updated = userMapper.updateAvatarById(userId, avatarUrl);
        if (updated != 1) {
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {
            }
            throw new BusinessException(ResultCode.AVATAR_UPLOAD_FAILED);
        }

        deleteOldAvatar(user.getAvatar());
        return toResponse(findUserById(userId));
    }

    /**
     * 上传用户背景图并更新 users.background_image 字段。
     */
    @Override
    @Transactional
    public UserResponse uploadBackgroundImage(Long userId, MultipartFile file) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.BACKGROUND_FILE_REQUIRED);
        }

        if (file.getSize() > backgroundMaxSizeBytes) {
            throw new BusinessException(ResultCode.BACKGROUND_SIZE_EXCEEDED);
        }

        String extension = resolveBackgroundExtension(file);
        String fileName = buildFileName(userId, extension);
        Path target = backgroundUploadPath.resolve(fileName).normalize();

        // 闃叉璺緞绌胯秺
        if (!target.startsWith(backgroundUploadPath)) {
            throw new BusinessException(ResultCode.BACKGROUND_UPLOAD_FAILED);
        }

        try {
            Files.createDirectories(backgroundUploadPath);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new BusinessException(ResultCode.BACKGROUND_UPLOAD_FAILED);
        }

        String oldBackgroundUrl = user.getBackgroundImage();
        String newUrl = backgroundPublicPath + "/" + fileName;

        userMapper.updateBackgroundImageById(userId, newUrl);
        user.setBackgroundImage(newUrl);

        try {
            deleteOldBackgroundImage(oldBackgroundUrl);
        } catch (Exception ignored) {
            // 椤界暐鍒犻櫎鑰佽儗鏅浘鏃圭殑寮傚父锛屼笉褰卞搷涓讳笟鍔°€?
        }

        return toResponse(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "userProfiles", key = "#userId")
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        userMapper.updateProfileById(userId, request.getNickname(), request.getBio(), request.getGender(),
                request.getBirthday(), request.getLocation());

        return toResponse(userMapper.selectById(userId));
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        if (userId == null || request == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        User user = findUserById(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_INCORRECT);
        }
        String newEncoded = passwordEncoder.encode(request.getNewPassword());
        userMapper.updatePasswordById(userId, newEncoded);
    }

    private User findUserById(Long id) {
        User user = userMapper.selectById(id);

        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        return user;
    }

    private User findUserByIdentifier(String identifier) {
        // 支持 5 位 ID、邮箱、用户名三种登录标识。
        if (identifier.matches("\\d{5}")) {
            return userMapper.selectById(Long.parseLong(identifier));
        }
        if (identifier.contains("@")) {
            return userMapper.selectByEmail(identifier);
        }
        return userMapper.selectByUsername(identifier);
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
        // 通过有限重试生成未占用的 5 位用户 ID。
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

    private static String buildAvatarFileName(Long userId, String extension) {
        return userId + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "")
                + "." + extension;
    }

    private static String buildFileName(Long userId, String extension) {
        return userId + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "")
                + "." + extension;
    }

    private static String normalizePublicPath(String publicPath) {
        if (!StringUtils.hasText(publicPath)) {
            return "/avatars";
        }

        String value = publicPath.trim();
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        if (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String resolveAvatarExtension(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String ext = null;
        if (StringUtils.hasText(originalName)) {
            int idx = originalName.lastIndexOf('.');
            if (idx >= 0 && idx < originalName.length() - 1) {
                ext = originalName.substring(idx + 1).toLowerCase(Locale.ROOT);
            }
        }

        if (ext != null && ALLOWED_AVATAR_EXTENSIONS.contains(ext)) {
            return ext;
        }

        // 文件名不可用时，回退到 Content-Type 判断。
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException(ResultCode.AVATAR_TYPE_INVALID);
        }

        if ("image/jpeg".equalsIgnoreCase(contentType)) {
            return "jpg";
        }
        if ("image/png".equalsIgnoreCase(contentType)) {
            return "png";
        }
        if ("image/gif".equalsIgnoreCase(contentType)) {
            return "gif";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return "webp";
        }

        throw new BusinessException(ResultCode.AVATAR_TYPE_INVALID);
    }

    private String resolveBackgroundExtension(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String ext = null;
        if (StringUtils.hasText(originalName)) {
            int idx = originalName.lastIndexOf('.');
            if (idx >= 0 && idx < originalName.length() - 1) {
                ext = originalName.substring(idx + 1).toLowerCase(Locale.ROOT);
            }
        }

        if (ext != null && ALLOWED_AVATAR_EXTENSIONS.contains(ext)) {
            return ext;
        }

        // 文件名不可用时，回退到 Content-Type 判断。
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessException(ResultCode.BACKGROUND_TYPE_INVALID);
        }

        if ("image/jpeg".equalsIgnoreCase(contentType)) {
            return "jpg";
        }
        if ("image/png".equalsIgnoreCase(contentType)) {
            return "png";
        }
        if ("image/gif".equalsIgnoreCase(contentType)) {
            return "gif";
        }
        if ("image/webp".equalsIgnoreCase(contentType)) {
            return "webp";
        }

        throw new BusinessException(ResultCode.BACKGROUND_TYPE_INVALID);
    }

    private void deleteOldAvatar(String oldAvatarUrl) {
        if (!StringUtils.hasText(oldAvatarUrl) || !oldAvatarUrl.startsWith(avatarPublicPath + "/")) {
            return;
        }

        String oldFileName = oldAvatarUrl.substring((avatarPublicPath + "/").length());
        if (!StringUtils.hasText(oldFileName) || oldFileName.contains("..") || oldFileName.contains("/")
                || oldFileName.contains("\\")) {
            return;
        }

        // 仅允许删除当前头像目录下的文件，避免误删。
        Path oldPath = avatarUploadPath.resolve(oldFileName).normalize();
        if (!oldPath.startsWith(avatarUploadPath)) {
            return;
        }

        try {
            Files.deleteIfExists(oldPath);
        } catch (IOException ignored) {
        }
    }

    private void deleteOldBackgroundImage(String oldBackgroundUrl) {
        if (!StringUtils.hasText(oldBackgroundUrl) || !oldBackgroundUrl.startsWith(backgroundPublicPath + "/")) {
            return;
        }

        String oldFileName = oldBackgroundUrl.substring((backgroundPublicPath + "/").length());
        if (!StringUtils.hasText(oldFileName) || oldFileName.contains("..") || oldFileName.contains("/")
                || oldFileName.contains("\\")) {
            return;
        }

        // 仅允许删除当前背景图目录下的文件，避免误删。
        Path oldPath = backgroundUploadPath.resolve(oldFileName).normalize();
        if (!oldPath.startsWith(backgroundUploadPath)) {
            return;
        }

        try {
            Files.deleteIfExists(oldPath);
        } catch (IOException ignored) {
        }
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
                user.getRole(),
                user.getLastLoginAt(),
                user.getCreatedAt());
    }

    private AuthResponse toAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = refreshTokenService.createRefreshToken(user.getId());
        return new AuthResponse(accessToken, refreshToken, "Bearer", jwtTokenProvider.getExpirationSeconds(), toResponse(user));
    }
}
