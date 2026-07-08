package com.worm.community_backend.moderation.mapper;

import com.worm.community_backend.moderation.entity.Report;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 举报数据访问层。
 */
@Mapper
public interface ReportMapper {

    @Insert("""
            INSERT INTO reports (reporter_id, target_type, target_id, reason, description, status, created_at)
            VALUES (#{reporterId}, #{targetType}, #{targetId}, #{reason}, #{description}, #{status}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Report report);

    @Select("""
            SELECT id, reporter_id, target_type, target_id, reason, description, status, 
                   reviewed_by, review_note, created_at, reviewed_at
            FROM reports
            WHERE id = #{id}
            """)
    Report selectById(Long id);

    @Select("""
            SELECT id, reporter_id, target_type, target_id, reason, description, status, 
                   reviewed_by, review_note, created_at, reviewed_at
            FROM reports
            WHERE reporter_id = #{reporterId} AND target_type = #{targetType} AND target_id = #{targetId} AND status = 'PENDING'
            """)
    Report selectPendingByReporterAndTarget(@Param("reporterId") Long reporterId, 
                                            @Param("targetType") String targetType, 
                                            @Param("targetId") Long targetId);

    @Select("""
            SELECT id, reporter_id, target_type, target_id, reason, description, status, 
                   reviewed_by, review_note, created_at, reviewed_at
            FROM reports
            WHERE status = #{status}
            ORDER BY created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<Report> selectByStatus(@Param("status") String status, 
                                @Param("offset") int offset, 
                                @Param("limit") int limit);

    @Select("""
            SELECT COUNT(*) FROM reports WHERE status = #{status}
            """)
    int countByStatus(String status);

    @Select("""
            SELECT id, reporter_id, target_type, target_id, reason, description, status, 
                   reviewed_by, review_note, created_at, reviewed_at
            FROM reports
            ORDER BY created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<Report> selectAll(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM reports")
    int countAll();

    @Update("""
            UPDATE reports
            SET status = #{status}, reviewed_by = #{reviewedBy}, review_note = #{reviewNote}, reviewed_at = #{reviewedAt}
            WHERE id = #{id}
            """)
    int updateStatus(@Param("id") Long id, 
                     @Param("status") String status, 
                     @Param("reviewedBy") Long reviewedBy,
                     @Param("reviewNote") String reviewNote,
                     @Param("reviewedAt") LocalDateTime reviewedAt);
}
