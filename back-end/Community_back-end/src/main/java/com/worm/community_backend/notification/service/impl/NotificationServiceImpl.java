package com.worm.community_backend.notification.service.impl;

import com.worm.community_backend.common.PageResponse;
import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.config.WsMessage;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.notification.entity.Notification;
import com.worm.community_backend.notification.mapper.NotificationMapper;
import com.worm.community_backend.notification.service.NotificationService;
import com.worm.community_backend.notification.vo.NotificationVO;
import com.worm.community_backend.notification.websocket.NotificationWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void createNotification(Long receiverId, Long senderId, String type, Long targetId) {
        if (receiverId == null || senderId == null || type == null) return;
        if (receiverId.equals(senderId)) return;

        Notification notification = new Notification();
        notification.setReceiverId(receiverId);
        notification.setSenderId(senderId);
        notification.setType(type);
        notification.setTargetId(targetId);

        notificationMapper.insert(notification);

        // 直接查询刚插入的通知并通过 WebSocket 推送
        try {
            NotificationVO vo = notificationMapper.selectById(notification.getId());
            if (vo != null) {
                WsMessage wsMsg = new WsMessage("NOTIFICATION", vo);
                String json = objectMapper.writeValueAsString(wsMsg);
                NotificationWebSocketHandler.sendToUser(receiverId, json);
            }
        } catch (Exception e) {
            log.warn("WebSocket 推送通知失败, receiverId={}", receiverId, e);
        }
    }

    @Override
    public List<NotificationVO> listNotifications(Long receiverId) {
        if (receiverId == null) throw new BusinessException(ResultCode.BAD_REQUEST);
        return notificationMapper.selectByReceiverId(receiverId);
    }

    @Override
    public PageResponse<NotificationVO> listNotificationsPage(Long receiverId, int page, int size) {
        if (receiverId == null) throw new BusinessException(ResultCode.BAD_REQUEST);
        int offset = (page - 1) * size;
        long total = notificationMapper.countByReceiverId(receiverId);
        List<NotificationVO> items = notificationMapper.selectByReceiverIdPage(receiverId, offset, size);
        boolean hasMore = offset + size < total;
        return new PageResponse<>(items, total, page, size, hasMore);
    }

    @Override
    public int countUnread(Long receiverId) {
        if (receiverId == null) throw new BusinessException(ResultCode.BAD_REQUEST);
        return notificationMapper.countUnread(receiverId);
    }

    @Override
    public void markAsRead(Long id, Long receiverId) {
        if (id == null || receiverId == null) throw new BusinessException(ResultCode.BAD_REQUEST);
        notificationMapper.markAsRead(id, receiverId);
    }

    @Override
    public void markAllAsRead(Long receiverId) {
        if (receiverId == null) throw new BusinessException(ResultCode.BAD_REQUEST);
        notificationMapper.markAllAsRead(receiverId);
    }
}
