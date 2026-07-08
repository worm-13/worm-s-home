package com.worm.community_backend.notification.controller;

import com.worm.community_backend.common.ApiResponse;
import com.worm.community_backend.common.PageResponse;
import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.notification.service.NotificationService;
import com.worm.community_backend.notification.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<PageResponse<NotificationVO>> listNotifications(
            Authentication auth,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(notificationService.listNotificationsPage(getUserId(auth), page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Integer>> countUnread(Authentication auth) {
        int count = notificationService.countUnread(getUserId(auth));
        return ApiResponse.success(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long id, Authentication auth) {
        notificationService.markAsRead(id, getUserId(auth));
        return ApiResponse.success(null);
    }

    @PutMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(Authentication auth) {
        notificationService.markAllAsRead(getUserId(auth));
        return ApiResponse.success(null);
    }

    private Long getUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        return userId;
    }
}
