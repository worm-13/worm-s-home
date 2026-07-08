package com.worm.community_backend.moderation.service;

import com.worm.community_backend.common.PageResponse;
import com.worm.community_backend.moderation.dto.ReportCreateDTO;
import com.worm.community_backend.moderation.vo.ReportVO;

/**
 * 举报服务接口。
 */
public interface ReportService {
    Long createReport(Long reporterId, ReportCreateDTO dto);
    PageResponse<ReportVO> listReports(String status, int page, int size);
    ReportVO reviewReport(Long reportId, Long reviewerId, String status, String reviewNote);
}
