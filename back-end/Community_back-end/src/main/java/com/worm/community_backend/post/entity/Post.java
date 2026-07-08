package com.worm.community_backend.post.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章实体，对应 posts 表。
 */
@Data
public class Post {
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String summary;
    private String coverImage;
    private Integer status;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    private LocalDateTime scheduledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

