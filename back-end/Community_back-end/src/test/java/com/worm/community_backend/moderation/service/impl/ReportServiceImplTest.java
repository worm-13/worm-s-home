package com.worm.community_backend.moderation.service.impl;

import com.worm.community_backend.common.PageResponse;
import com.worm.community_backend.exception.BusinessException;
import com.worm.community_backend.moderation.dto.ReportCreateDTO;
import com.worm.community_backend.moderation.entity.Report;
import com.worm.community_backend.moderation.mapper.ReportMapper;
import com.worm.community_backend.moderation.vo.ReportVO;
import com.worm.community_backend.user.entity.User;
import com.worm.community_backend.user.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ReportMapper reportMapper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private ReportServiceImpl reportService;

    private User testReporter;
    private User testReviewer;
    private Report testReport;

    @BeforeEach
    void setUp() {
        testReporter = User.builder()
                .id(1L)
                .username("reporter")
                .nickname("Reporter")
                .build();

        testReviewer = User.builder()
                .id(2L)
                .username("reviewer")
                .nickname("Reviewer")
                .build();

        testReport = Report.builder()
                .id(1L)
                .reporterId(1L)
                .targetType("POST")
                .targetId(100L)
                .reason("SPAM")
                .description("This is spam content")
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createReport_Success() {
        ReportCreateDTO dto = new ReportCreateDTO();
        dto.setTargetType("POST");
        dto.setTargetId(100L);
        dto.setReason("SPAM");
        dto.setDescription("This is spam content");

        when(reportMapper.selectPendingByReporterAndTarget(1L, "POST", 100L)).thenReturn(null);
        doAnswer(invocation -> {
            Report r = invocation.getArgument(0);
            r.setId(1L);
            return 1;
        }).when(reportMapper).insert(any(Report.class));

        Long reportId = reportService.createReport(1L, dto);

        assertNotNull(reportId);
        verify(reportMapper).insert(any(Report.class));
    }

    @Test
    void createReport_AlreadyReported() {
        ReportCreateDTO dto = new ReportCreateDTO();
        dto.setTargetType("POST");
        dto.setTargetId(100L);
        dto.setReason("SPAM");

        when(reportMapper.selectPendingByReporterAndTarget(1L, "POST", 100L)).thenReturn(testReport);

        assertThrows(BusinessException.class, () -> reportService.createReport(1L, dto));
    }

    @Test
    void listReports_WithStatus() {
        ReportVO reportVO = ReportVO.builder()
                .id(1L)
                .reporterId(1L)
                .reporterName("Reporter")
                .targetType("POST")
                .targetId(100L)
                .reason("SPAM")
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        when(reportMapper.selectByStatus("PENDING", 0, 20)).thenReturn(Arrays.asList(testReport));
        when(reportMapper.countByStatus("PENDING")).thenReturn(1);
        when(userMapper.selectById(1L)).thenReturn(testReporter);

        PageResponse<ReportVO> response = reportService.listReports("PENDING", 1, 20);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals("PENDING", response.getItems().get(0).getStatus());
    }

    @Test
    void reviewReport_Success() {
        when(reportMapper.selectById(1L)).thenReturn(testReport);
        when(reportMapper.updateStatus(eq(1L), eq("RESOLVED"), eq(2L), eq("Valid report"), any(LocalDateTime.class))).thenReturn(1);
        
        Report reviewedReport = Report.builder()
                .id(1L)
                .reporterId(1L)
                .targetType("POST")
                .targetId(100L)
                .reason("SPAM")
                .status("RESOLVED")
                .reviewedBy(2L)
                .reviewNote("Valid report")
                .reviewedAt(LocalDateTime.now())
                .build();
        
        when(reportMapper.selectById(1L)).thenReturn(reviewedReport);
        when(userMapper.selectById(1L)).thenReturn(testReporter);
        when(userMapper.selectById(2L)).thenReturn(testReviewer);

        ReportVO result = reportService.reviewReport(1L, 2L, "RESOLVED", "Valid report");

        assertNotNull(result);
        assertEquals("RESOLVED", result.getStatus());
        assertEquals("Reviewer", result.getReviewerName());
    }

    @Test
    void reviewReport_NotFound() {
        when(reportMapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> reportService.reviewReport(999L, 2L, "RESOLVED", "Valid report"));
    }
}
