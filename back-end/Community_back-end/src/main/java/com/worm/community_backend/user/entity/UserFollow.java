package com.worm.community_backend.user.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserFollow {
    private Long id;
    private Long followerId;
    private Long followingId;
    private LocalDateTime createdAt;
}
