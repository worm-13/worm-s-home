package com.worm.community_backend.moderation.service.impl;

import com.worm.community_backend.common.PageResponse;
import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.moderation.dto.ReportCreateDTO;
import com.worm.community_backend.moderation.entity.Report;
import com.worm.community_backend.moderation.mapper.ReportMapper;
import com.worm.community_backend.moderation.service.ReportService;
import com.worm.community_backend.moderation.vo.ReportVO;
import com.worm.community_backend.user.mapper.UserMapper;
import com.worm.community_backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 举报服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public Long createReport(Long reporterId, ReportCreateDTO dto) {
        // 检查是否已举报过
        Report existing = reportMapper.selectPendingByReporterAndTarget(
                reporterId, dto.getTargetType(), dto.getTargetId());
        if (existing != null) {
            throw new BusinessException(1046, "already reported this content");
        }

        Report report = Report.builder()
                .reporterId(reporterId)
                .targetType(dto.getTargetType())
                .targetId(dto.getTargetId())
                .reason(dto.getReason())
                .description(dto.getDescription())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        reportMapper.insert(report);
        log.info("User {} reported {} {}", reporterId, dto.getTargetType(), dto.getTargetId());
        return report.getId();
    }

    @Override
    public PageResponse<ReportVO> listReports(String status, int page, int size) {
        int offset = (page - 1) * size;
        List<Report> reports;
        int total;

        if (status != null && !status.isEmpty()) {
            reports = reportMapper.selectByStatus(status, offset, size);
            total = reportMapper.countByStatus(status);
        } else {
            reports = reportMapper.selectAll(offset, size);
            total = reportMapper.countAll();
        }

        List<ReportVO> items = reports.stream()
                .map(this::toVO)
                .collect(Collectors.toList());

        return new PageResponse<>(items, total, page, size, offset + size < total);
    }

    @Override
    @Transactional
    public ReportVO reviewReport(Long reportId, Long reviewerId, String status, String reviewNote) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        reportMapper.updateStatus(reportId, status, reviewerId, reviewNote, LocalDateTime.now());
        log.info("Report {} reviewed by {}: {}", reportId, reviewerId, status);

        return toVO(reportMapper.selectById(reportId));
    }

    private ReportVO toVO(Report report) {
        User reporter = userMapper.selectById(report.getReporterId());
        User reviewer = report.getReviewedBy() != null ? userMapper.selectById(report.getReviewedBy()) : null;

        return ReportVO.builder()
                .id(report.getId())
                .reporterId(report.getReporterId())
                .reporterName(reporter != null ? reporter.getNickname() : "unknown")
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reason(report.getReason())
                .description(report.getDescription())
                .status(report.getStatus())
                .reviewedBy(report.getReviewedBy())
                .reviewerName(reviewer != null ? reviewer.getNickname() : null)
                .reviewNote(report.getReviewNote())
                .createdAt(report.getCreatedAt())
                .reviewedAt(report.getReviewedAt())
                .build();
    }
}
