package com.worm.community_backend.post.dto;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * 发布文章请求参数。
 * status: 1=发布, 2=草稿, 3=定时发布(需配合scheduledAt)
 */
@Data
public class PostCreateDTO {
    @NotBlank(message = "post title is required")
    private String title;
    @NotBlank(message = "post content is required")
    private String content;
    private String summary;
    private String coverImage;
    /** 文章状态：1-发布 2-草稿 3-定时发布，默认1 */
    private Integer status;
    /** 定时发布时间，status=3时必填 */
    private LocalDateTime scheduledAt;
}
