package com.worm.community_backend.moderation.controller;

import com.worm.community_backend.common.ApiResponse;
import com.worm.community_backend.common.PageResponse;
import com.worm.community_backend.common.ResultCode;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.moderation.dto.ReportCreateDTO;
import com.worm.community_backend.moderation.service.ReportService;
import com.worm.community_backend.moderation.vo.ReportVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 举报控制器。
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 创建举报。
     */
    @PostMapping
    public ApiResponse<Map<String, Long>> createReport(@Valid @RequestBody ReportCreateDTO dto, Authentication auth) {
        Long reportId = reportService.createReport(getUserId(auth), dto);
        return ApiResponse.success(Map.of("reportId", reportId));
    }

    /**
     * 获取举报列表（管理员）。
     */
    @GetMapping
    public ApiResponse<PageResponse<ReportVO>> listReports(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(reportService.listReports(status, page, size));
    }

    /**
     * 审核举报（管理员）。
     */
    @PutMapping("/{id}/review")
    public ApiResponse<ReportVO> reviewReport(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        String status = body.get("status");
        String reviewNote = body.get("reviewNote");
        return ApiResponse.success(reportService.reviewReport(id, getUserId(auth), status, reviewNote));
    }

    private Long getUserId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        return userId;
    }
}
