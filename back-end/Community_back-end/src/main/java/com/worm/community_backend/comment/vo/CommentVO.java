package com.worm.community_backend.comment.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommentVO {
    private Long id;
    private Long postId;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private Long parentId;
    private String content;
    private LocalDateTime createdAt;
    private List<CommentVO> replies;
}
