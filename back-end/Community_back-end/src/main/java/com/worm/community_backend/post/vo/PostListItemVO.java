package com.worm.community_backend.post.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章列表项返回对象。
 */
@Data
public class PostListItemVO {
    private Long id;
    private Long userId;
    private String authorName;
    private String authorAvatar;
    private String title;
    private String summary;
    private String coverImage;
    /** 文章状态：1-已发布 2-草稿 3-定时发布 */
    private Integer status;
    /** 定时发布时间 */
    private LocalDateTime scheduledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
