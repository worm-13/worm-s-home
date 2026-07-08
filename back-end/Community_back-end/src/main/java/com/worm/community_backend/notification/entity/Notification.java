package com.worm.community_backend.notification.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Notification {
    private Long id;
    private Long receiverId;
    private Long senderId;
    private String type;
    private Long targetId;
    private Integer isRead;
    private LocalDateTime createdAt;
}
