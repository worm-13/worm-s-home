package com.worm.community_backend.moderation.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 举报实体，对应 reports 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {
    private Long id;
    private Long reporterId;
    private String targetType;  // POST, COMMENT, USER
    private Long targetId;
    private String reason;      // SPAM, HARASSMENT, INAPPROPRIATE, OTHER
    private String description;
    private String status;      // PENDING, REVIEWED, DISMISSED, RESOLVED
    private Long reviewedBy;
    private String reviewNote;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
