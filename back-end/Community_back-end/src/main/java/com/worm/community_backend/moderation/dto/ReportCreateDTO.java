package com.worm.community_backend.moderation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 举报请求 DTO。
 */
@Data
public class ReportCreateDTO {
    @NotBlank(message = "target type is required")
    private String targetType;  // POST, COMMENT, USER

    @NotNull(message = "target id is required")
    private Long targetId;

    @NotBlank(message = "reason is required")
    private String reason;  // SPAM, HARASSMENT, INAPPROPRIATE, OTHER

    @Size(max = 500, message = "description must be less than 500 characters")
    private String description;
}
