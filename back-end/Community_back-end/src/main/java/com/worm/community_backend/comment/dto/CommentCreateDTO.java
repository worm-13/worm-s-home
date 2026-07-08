package com.worm.community_backend.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentCreateDTO {
    @NotNull
    private Long postId;
    private Long parentId;
    @NotBlank
    private String content;
}
