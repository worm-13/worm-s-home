package com.worm.community_backend.user.controller;

import com.worm.community_backend.common.ApiResponse;
import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.user.dto.ChangePasswordRequest;
import com.worm.community_backend.user.dto.LoginRequest;
import com.worm.community_backend.user.dto.RefreshTokenRequest;
import com.worm.community_backend.user.dto.RegisterRequest;
import com.worm.community_backend.user.dto.UpdateProfileRequest;
import com.worm.community_backend.user.service.UserService;
import com.worm.community_backend.user.vo.AuthResponse;
import com.worm.community_backend.user.vo.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(userService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(userService.login(request));
    }

    @PostMapping("/refresh-token")
    public ApiResponse<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(userService.refreshToken(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(Authentication auth) {
        userService.logout(getUserId(auth));
        return ApiResponse.success(null);
    }

    @GetMapping("/users/{id}")
    public ApiResponse<UserResponse> getUserById(@PathVariable Long id) {
        return ApiResponse.success(userService.getUserById(id));
    }

    @PostMapping(value = "/users/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserResponse> uploadAvatar(@RequestParam("file") MultipartFile file, Authentication auth) {
        return ApiResponse.success(userService.uploadAvatar(getUserId(auth), file));
    }

    @PostMapping(value = "/users/me/background-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserResponse> uploadBackgroundImage(@RequestParam("file") MultipartFile file, Authentication auth) {
        return ApiResponse.success(userService.uploadBackgroundImage(getUserId(auth), file));
    }

    @PutMapping("/users/me/profile")
    public ApiResponse<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request, Authentication auth) {
        return ApiResponse.success(userService.updateProfile(getUserId(auth), request));
    }

    @PutMapping("/users/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication auth) {
        userService.changePassword(getUserId(auth), request);
        return ApiResponse.success(null);
    }

    private Long getUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        return userId;
    }
}
