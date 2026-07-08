package com.worm.community_backend.message.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MessageVO {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private String senderName;
    private String senderAvatar;
}
