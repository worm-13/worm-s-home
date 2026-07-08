package com.worm.community_backend.notification.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationVO {
    private Long id;
    private String type;
    private Long targetId;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String postTitle;
}
