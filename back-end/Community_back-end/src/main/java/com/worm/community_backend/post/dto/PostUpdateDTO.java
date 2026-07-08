package com.worm.community_backend.post.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PostUpdateDTO {
    private String title;
    private String content;
    private String summary;
    private String coverImage;
    /** 文章状态：1-发布 2-草稿 3-定时发布 */
    private Integer status;
    /** 定时发布时间，status=3时必填 */
    private LocalDateTime scheduledAt;
}
