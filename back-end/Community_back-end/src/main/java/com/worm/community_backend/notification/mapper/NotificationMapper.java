package com.worm.community_backend.notification.mapper;

import com.worm.community_backend.notification.entity.Notification;
import com.worm.community_backend.notification.vo.NotificationVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {

    int insert(Notification notification);

    NotificationVO selectById(@Param("id") Long id);

    List<NotificationVO> selectByReceiverId(@Param("receiverId") Long receiverId);

    List<NotificationVO> selectByReceiverIdPage(@Param("receiverId") Long receiverId, @Param("offset") int offset, @Param("limit") int limit);

    long countByReceiverId(@Param("receiverId") Long receiverId);

    int countUnread(@Param("receiverId") Long receiverId);

    int markAsRead(@Param("id") Long id, @Param("receiverId") Long receiverId);

    int markAllAsRead(@Param("receiverId") Long receiverId);
}
