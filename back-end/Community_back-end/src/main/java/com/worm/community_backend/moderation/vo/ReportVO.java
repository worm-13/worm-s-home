package com.worm.community_backend.moderation.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 举报响应 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportVO {
    private Long id;
    private Long reporterId;
    private String reporterName;
    private String targetType;
    private Long targetId;
    private String targetTitle;
    private String reason;
    private String description;
    private String status;
    private Long reviewedBy;
    private String reviewerName;
    private String reviewNote;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
