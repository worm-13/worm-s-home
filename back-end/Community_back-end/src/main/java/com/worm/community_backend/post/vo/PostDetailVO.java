package com.worm.community_backend.post.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章详情返回对象。
 */
@Data
public class PostDetailVO {
    private Long id;
    private Long userId;
    private String authorName;
    private String authorAvatar;
    private String title;
    private String content;
    private String summary;
    private String coverImage;
    /** 文章状态：1-已发布 2-草稿 3-定时发布 */
    private Integer status;
    /** 定时发布时间 */
    private LocalDateTime scheduledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer likesCount;
    private Integer favoritesCount;
    private Boolean isLiked;
    private Boolean isFavorited;
}
