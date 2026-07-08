package com.worm.community_backend.message.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConversationVO {
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private int unreadCount;
}
