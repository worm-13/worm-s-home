package com.worm.community_backend.notification.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worm.community_backend.common.PageResponse;
import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.notification.entity.Notification;
import com.worm.community_backend.notification.mapper.NotificationMapper;
import com.worm.community_backend.notification.vo.NotificationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private final Long receiverId = 200L;
    private final Long senderId = 100L;
    private final Long notificationId = 1L;

    @Test
    void createNotification_Success() throws Exception {
        doAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(notificationId);
            return 1;
        }).when(notificationMapper).insert(any(Notification.class));

        NotificationVO vo = new NotificationVO();
        vo.setId(notificationId);
        vo.setSenderId(senderId);
        vo.setType("LIKE");

        when(notificationMapper.selectById(notificationId)).thenReturn(vo);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        notificationService.createNotification(receiverId, senderId, "LIKE", 1L);

        verify(notificationMapper).insert(any(Notification.class));
        verify(notificationMapper).selectById(notificationId);
    }

    @Test
    void createNotification_SameUser() {
        notificationService.createNotification(senderId, senderId, "LIKE", 1L);

        verify(notificationMapper, never()).insert(any(Notification.class));
    }

    @Test
    void createNotification_NullParams() {
        notificationService.createNotification(null, senderId, "LIKE", 1L);
        notificationService.createNotification(receiverId, null, "LIKE", 1L);
        notificationService.createNotification(receiverId, senderId, null, 1L);

        verify(notificationMapper, never()).insert(any(Notification.class));
    }

    @Test
    void listNotifications_Success() {
        NotificationVO vo = new NotificationVO();
        vo.setId(notificationId);
        vo.setType("LIKE");

        when(notificationMapper.selectByReceiverId(receiverId)).thenReturn(Arrays.asList(vo));

        List<NotificationVO> notifications = notificationService.listNotifications(receiverId);

        assertNotNull(notifications);
        assertEquals(1, notifications.size());
        assertEquals(notificationId, notifications.get(0).getId());
    }

    @Test
    void listNotifications_NullReceiverId() {
        assertThrows(BusinessException.class, () -> notificationService.listNotifications(null));
    }

    @Test
    void listNotificationsPage_Success() {
        NotificationVO vo = new NotificationVO();
        vo.setId(notificationId);
        vo.setType("LIKE");

        when(notificationMapper.countByReceiverId(receiverId)).thenReturn(1L);
        when(notificationMapper.selectByReceiverIdPage(receiverId, 0, 20)).thenReturn(Arrays.asList(vo));

        PageResponse<NotificationVO> response = notificationService.listNotificationsPage(receiverId, 1, 20);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals(1L, response.getTotal());
        assertFalse(response.isHasMore());
    }

    @Test
    void countUnread_Success() {
        when(notificationMapper.countUnread(receiverId)).thenReturn(3);

        int count = notificationService.countUnread(receiverId);

        assertEquals(3, count);
    }

    @Test
    void countUnread_NullReceiverId() {
        assertThrows(BusinessException.class, () -> notificationService.countUnread(null));
    }

    @Test
    void markAsRead_Success() {
        notificationService.markAsRead(notificationId, receiverId);

        verify(notificationMapper).markAsRead(notificationId, receiverId);
    }

    @Test
    void markAsRead_NullId() {
        assertThrows(BusinessException.class, () -> notificationService.markAsRead(null, receiverId));
    }

    @Test
    void markAllAsRead_Success() {
        notificationService.markAllAsRead(receiverId);

        verify(notificationMapper).markAllAsRead(receiverId);
    }

    @Test
    void markAllAsRead_NullReceiverId() {
        assertThrows(BusinessException.class, () -> notificationService.markAllAsRead(null));
    }
}
