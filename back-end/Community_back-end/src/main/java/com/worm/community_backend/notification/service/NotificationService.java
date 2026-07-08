package com.worm.community_backend.notification.service;

import com.worm.community_backend.common.PageResponse;
import com.worm.community_backend.notification.vo.NotificationVO;

import java.util.List;

public interface NotificationService {
    void createNotification(Long receiverId, Long senderId, String type, Long targetId);
    List<NotificationVO> listNotifications(Long receiverId);
    PageResponse<NotificationVO> listNotificationsPage(Long receiverId, int page, int size);
    int countUnread(Long receiverId);
    void markAsRead(Long id, Long receiverId);
    void markAllAsRead(Long receiverId);
}
